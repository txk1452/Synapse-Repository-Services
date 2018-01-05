package org.sagebionetworks.search;

import static org.sagebionetworks.search.SearchConstants.FIELD_ETAG;
import static org.sagebionetworks.search.SearchConstants.FIELD_ID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.services.cloudsearchdomain.model.Hit;
import com.amazonaws.services.cloudsearchdomain.model.QueryParser;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.google.common.collect.Sets;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import javax.print.Doc;

/**
 * Implementation of the Search DAO.
 * 
 * @author jmhill
 * 
 */
public class SearchDaoImpl implements SearchDao {

	private static final String QUERY_BY_ID_AND_ETAG = "(and "+FIELD_ID+":'%1$s' "+FIELD_ETAG+":'%2$s')";

	private static final String QUERY_LIST_ALL_DOCUMENTS_ONE_PAGE = FIELD_ID+":'*'";

	static private Logger log = LogManager.getLogger(SearchDaoImpl.class);

	CloudSearchClientProvider cloudSearchClientProvider;


	@Override
	public void deleteDocument(String documentId) {
		// This is just a batch delete of size one.
		deleteDocuments(Sets.newHashSet(documentId));
	}

	@Override
	public void deleteDocuments(Set<String> docIdsToDelete) { //TODO: check if autowire test exists
		DateTime now = DateTime.now();
		// Note that we cannot use a JSONEntity here because the format is
		// just a JSON array
		List<Document> documentBatch = new ArrayList<>(docIdsToDelete.size());
		for (String entityId : docIdsToDelete) {
			Document document = new Document();
			document.setType(DocumentTypeNames.delete);
			document.setId(entityId);
			document.setVersion(now.getMillis() / 1000);
			documentBatch.add(document);
		}
		// Delete the batch.
		sendDocuments(documentBatch);
	}

	@Override
	public void sendDocument(Document document){
		sendDocuments(Collections.singletonList(document));
	}

	@Override
	public void sendDocuments(List<Document> document){
		cloudSearchClientProvider.getCloudSearchClient().sendDocuments(document);
	}

	@Override
	public SearchResult executeSearch(SearchRequest search) throws CloudSearchClientException {
		CloudsSearchDomainClientAdapter searchClient = cloudSearchClientProvider.getCloudSearchClient();
		return searchClient.rawSearch(search);
	}

	@Override
	public boolean doesDocumentExist(String id, String etag) throws CloudSearchClientException {
		// Search for the document
		String query = String.format(QUERY_BY_ID_AND_ETAG, id, etag);
		SearchResult results = executeSearch(new SearchRequest().withQuery(query).withQueryParser(QueryParser.Structured));
		return results.getHits().getFound() > 0;
	}

	@Override //TODO: make package scope?
	public SearchResult listSearchDocuments(long limit, long offset) throws CloudSearchClientException {
		return executeSearch(new SearchRequest().withQuery(QUERY_LIST_ALL_DOCUMENTS_ONE_PAGE)
				.withQueryParser(QueryParser.Structured)
				.withSize(limit).withStart(offset));
	}

	@Override
	public void deleteAllDocuments() throws CloudSearchClientException {
		// Keep deleting as long as there are documents
		SearchResult sr = null;
		do{
			sr = listSearchDocuments(1000, 0);
			HashSet<String> idSet = new HashSet<String>();
			for(Hit hit: sr.getHits().getHit()){
				idSet.add(hit.getId());
			}
			// Delete the whole set
			if(!idSet.isEmpty()){
				log.warn("Deleting the following documents from the search index:"+idSet.toString());
				deleteDocuments(idSet);
				Thread.sleep(5000);
			}
		}while(sr.getHits().getFound() > 0);
	}
}
