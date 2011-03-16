package org.sagebionetworks.repo.model.jdo;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;



@PersistenceCapable(detachable = "false")
public class JDOResourceAccess {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.SEQUENCE, sequence="GLOBAL_SEQ")
	private Long id;
	
	@Persistent
	private JDOUserGroup owner;
	
	@Persistent
	private Long resource;
		
	// e.g. read, change, share
	@Persistent
	private String accessType;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public JDOUserGroup getOwner() {
		return owner;
	}

	public void setOwner(JDOUserGroup owner) {
		this.owner = owner;
	}

	public Long getResource() {
		return resource;
	}

	public void setResource(Long resource) {
		this.resource = resource;
	}

	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}
	
	
}
