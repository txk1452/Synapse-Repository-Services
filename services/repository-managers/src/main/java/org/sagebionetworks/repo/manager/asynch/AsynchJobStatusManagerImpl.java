package org.sagebionetworks.repo.manager.asynch;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class AsynchJobStatusManagerImpl implements AsynchJobStatusManager {
	
	private static final String CACHED_MESSAGE_TEMPLATE = "Returning a cached job for user: %d, requestHash: %s, objectEtag: %s, and jobId: %s";


	static private Log log = LogFactory.getLog(AsynchJobStatusManagerImpl.class);	
	
	
	private static final String JOB_ABORTED_MESSAGE = "Job aborted because the stack was not in: "+StatusEnum.READ_WRITE;

	@Autowired
	AsynchronousJobStatusDAO asynchJobStatusDao;
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	StackStatusDao stackStatusDao;
	@Autowired
	AsynchJobQueuePublisher asynchJobQueuePublisher;
	@Autowired
	JobHashProvider jobHashProvider;
	
	@Override
	public AsynchronousJobStatus getJobStatus(UserInfo userInfo, String jobId) throws DatastoreException, NotFoundException {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		// Get the status
		AsynchronousJobStatus status = asynchJobStatusDao.getJobStatus(jobId);
		// Only the user that started a job can read it
		if(!authorizationManager.isUserCreatorOrAdmin(userInfo, status.getStartedByUserId().toString())){
			throw new UnauthorizedException("Only the user that created a job can access the job's status.");
		}
		// If a job is running and the stack is not in READ-WRITE mode then the job is failed.
		if(AsynchJobState.PROCESSING.equals(status.getJobState())){
			// Since the job is processing check the state of the stack.
			checkStackReadWrite();
		}
		return status;
	}

	@Override
	public AsynchronousJobStatus startJob(UserInfo user, AsynchronousRequestBody body) throws DatastoreException, NotFoundException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(body == null) throw new IllegalArgumentException("Body cannot be null");
		/*
		 *  Before we start the job, we need to determine if a job already exists
		 *  for this request, object etag, and user.
		 */
		String requestHash = jobHashProvider.getJobHash(body);
		// Lookup the current etag for the request object.
		String objectEtag = jobHashProvider.getRequestObjectEtag(body);
		// Does this job already exist
		AsynchronousJobStatus status = findJobsMatching(requestHash, objectEtag, body, user.getId());
		if(status != null){
			/*
			 * If here then the caller has already made this exact request
			 * and the object has not changed since the last request.
			 * Therefore, we return the same job status as before without
			 * starting a new job.
			 */
			log.info(String.format(CACHED_MESSAGE_TEMPLATE, user.getId(), requestHash, objectEtag, status.getJobId()));
			return status;
		}
		// Start the job.
		status = asynchJobStatusDao.startJob(user.getId(), body, requestHash, objectEtag);
		// publish a message to get the work started
		asynchJobQueuePublisher.publishMessage(status);
		return status;
	}
	
	/**
	 * Find a job that matches the given requestHash, objectEtag, body and userId.
	 * 
	 * @param requestHash
	 * @param objectEtag
	 * @param body
	 * @param userId
	 * @return
	 */
	private AsynchronousJobStatus findJobsMatching(String requestHash, String objectEtag, AsynchronousRequestBody body, Long userId){
		if (objectEtag != null) {
			// Find all jobs that match this request.
			List<AsynchronousJobStatus> matches = asynchJobStatusDao
					.findCompletedJobStatus(requestHash, objectEtag, userId);
			if (matches != null) {
				for(AsynchronousJobStatus match: matches){
					if(body.equals(match.getRequestBody())){
						return match;
					}
				}
			}
		}
		// no match found
		return null;
	}


	@WriteTransaction
	@Override
	public void updateJobProgress(String jobId, Long progressCurrent, Long progressTotal, String progressMessage) {
		// Progress can only be updated if the stack is in read-write mode.
		checkStackReadWrite();
		asynchJobStatusDao.updateJobProgress(jobId, progressCurrent, progressTotal, progressMessage);
	}

	/**
	 * If the stack is not in read-write mode an IllegalStateException will be thrown.
	 */
	private void checkStackReadWrite() {
		if(!StatusEnum.READ_WRITE.equals(stackStatusDao.getCurrentStatus())){
			throw new IllegalStateException(JOB_ABORTED_MESSAGE);
		}
	}


	@WriteTransaction
	@Override
	public String setJobFailed(String jobId, Throwable error) {
		// We allow a job to fail even if the stack is not in read-write mode.
		return asynchJobStatusDao.setJobFailed(jobId, error);
	}


	@WriteTransaction
	@Override
	public String setComplete(String jobId, AsynchronousResponseBody body)
			throws DatastoreException, NotFoundException {
		// Job can only be completed if the stack is in read-write mode.
		checkStackReadWrite();
		return asynchJobStatusDao.setComplete(jobId, body);
	}

	@Override
	public void emptyAllQueues() {
		asynchJobQueuePublisher.emptyAllQueues();
	}
	
	

}
