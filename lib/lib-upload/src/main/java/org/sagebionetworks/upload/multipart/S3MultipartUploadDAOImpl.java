package org.sagebionetworks.upload.multipart;

import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.PartMD5;
import org.sagebionetworks.upload.UploadUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.util.BinaryUtils;

public class S3MultipartUploadDAOImpl implements S3MultipartUploadDAO {

	// 15 minute.
	private static final int PRE_SIGNED_URL_EXPIRATION_MS = 1000 * 60 * 15;

	@Autowired
	private AmazonS3 s3Client;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.upload.multipart.S3MultipartUploadDAO#
	 * initiateMultipartUpload(java.lang.String, java.lang.String,
	 * org.sagebionetworks.repo.model.file.MultipartUploadRequest)
	 */
	@Override
	public String initiateMultipartUpload(String bucket, String key,
			MultipartUploadRequest request) {
		String contentType = request.getContentType();
		if (contentType == null) {
			contentType = "application/octet-stream";
		}
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentType(contentType);
		objectMetadata.setContentDisposition(UploadUtils
				.getContentDispositionValue(request.getFileName()));
		objectMetadata.setContentMD5(BinaryUtils.toBase64(BinaryUtils
				.fromHex(request.getContentMD5Hex())));
		InitiateMultipartUploadResult result = s3Client
				.initiateMultipartUpload(new InitiateMultipartUploadRequest(
						bucket, key, objectMetadata)
				.withCannedACL(CannedAccessControlList.BucketOwnerFullControl));
		return result.getUploadId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.upload.multipart.S3MultipartUploadDAO#
	 * createPreSignedPutUrl(java.lang.String, java.lang.String)
	 */
	@Override
	public URL createPreSignedPutUrl(String bucket, String partKey, String contentType) {
		long expiration = System.currentTimeMillis()+ PRE_SIGNED_URL_EXPIRATION_MS;
		GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
				bucket, partKey).withMethod(HttpMethod.PUT).withExpiration(
				new Date(expiration));
		/*
		 * Adding 'Expires' to the signed url is a hack we were forced to 
		 * add due to SYNPY-409 (see also PLFM-4183)
		 */
		request.addRequestParameter("Expires", ""+(expiration/1000));
		if(contentType != null){
			request.setContentType(contentType);
		}
		return s3Client.generatePresignedUrl(request);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.upload.multipart.S3MultipartUploadDAO#addPart(org
	 * .sagebionetworks.upload.multipart.AddPartRequest)
	 */
	@Override
	public void addPart(AddPartRequest request) {
		CopyPartRequest cpr = new CopyPartRequest();
		cpr.setSourceBucketName(request.getBucket());
		cpr.setSourceKey(request.getPartKey());
		cpr.setDestinationKey(request.getKey());
		cpr.setDestinationBucketName(request.getBucket());
		cpr.setUploadId(request.getUploadToken());
		cpr.setPartNumber(request.getPartNumber());
		// only add if the etag matches.
		cpr.withMatchingETagConstraint(request.getPartMD5Hex());
		CopyPartResult result = s3Client.copyPart(cpr);
		if (result == null) {
			throw new IllegalArgumentException(
					"The provided MD5 does not match the MD5 of the uploaded part.  Please re-upload the part.");
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.upload.multipart.S3MultipartUploadDAO#deleteObject
	 * (java.lang.String, java.lang.String)
	 */
	@Override
	public void deleteObject(String bucket, String key) {
		s3Client.deleteObject(bucket, key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.upload.multipart.S3MultipartUploadDAO#
	 * completeMultipartUpload
	 * (org.sagebionetworks.repo.model.file.CompleteMultipartRequest)
	 */
	@Override
	public long completeMultipartUpload(CompleteMultipartRequest request) {
		CompleteMultipartUploadRequest cmur = new CompleteMultipartUploadRequest();
		cmur.setBucketName(request.getBucket());
		cmur.setKey(request.getKey());
		cmur.setUploadId(request.getUploadToken());
		// convert the parts MD5s to etags
		List<PartETag> partEtags = new LinkedList<PartETag>();
		cmur.setPartETags(partEtags);
		for (PartMD5 partMD5 : request.getAddedParts()) {
			String partEtag = partMD5.getPartMD5Hex();
			partEtags.add(new PartETag(partMD5.getPartNumber(), partEtag));
		}

		try {
			s3Client.completeMultipartUpload(cmur);
		} catch (AmazonS3Exception e) {
			// thrown when given a bad request.
			throw new IllegalArgumentException(e.getMessage());
		}
		// Lookup the final size of this file
		ObjectMetadata resultFileMetadata = s3Client.getObjectMetadata(
				request.getBucket(), request.getKey());
		return resultFileMetadata.getContentLength();
	}

}
