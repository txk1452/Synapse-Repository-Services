package org.sagebionetworks.repo.model;

import java.util.Date;


/**
 * 
 * 
 * @author bhoff
 * 
 */
public interface Base {
	public void setId(String id);

	public String getId();

	public void setUri(String uri);

	public String getUri();

	public void setEtag(String etag);

	public String getEtag();
	

	/**
	 * @param createDate
	 */
	public void setCreationDate(Date createDate);

	/**
	 * @return the creation date
	 */
	public Date getCreationDate();
	

	

}
