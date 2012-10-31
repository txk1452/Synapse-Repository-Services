package org.sagebionetworks.competition.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.sagebionetworks.competition.dbo.DBOConstants;
import org.sagebionetworks.competition.dbo.SubmissionDBO;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.query.jdo.SQLConstants;
import org.sagebionetworks.competition.util.Utility;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SubmissionDAOImpl {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	public static final String ID = DBOConstants.PARAM_SUBMISSION_ID;
	public static final String USER_ID = DBOConstants.PARAM_SUBMISSION_USER_ID;
	public static final String COMP_ID = DBOConstants.PARAM_SUBMISSION_COMP_ID;
	public static final String STATUS = DBOConstants.PARAM_SUBMISSION_STATUS;
	
	private static final String SELECT_BY_USER_SQL = 
			"SELECT * FROM "+ SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_SUBMISSION_USER_ID + "=:"+ USER_ID;
	
	private static final String SELECT_BY_COMPETITION_SQL = 
			"SELECT * FROM "+ SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_SUBMISSION_COMP_ID + "=:"+ COMP_ID;
	
	private static final String SELECT_BY_COMPETITION_AND_STATUS_SQL = 
			"SELECT * FROM "+ SQLConstants.TABLE_SUBMISSION +
			" WHERE "+ SQLConstants.COL_SUBMISSION_COMP_ID + "=:"+ COMP_ID +
			" AND " + SQLConstants.COL_SUBMISSION_STATUS + "=:" + STATUS;
	
	private static final RowMapper<SubmissionDBO> rowMapper = ((new SubmissionDBO()).getTableMapping());

	public void create(Submission dto) throws DatastoreException {		
		// Convert to DBO
		SubmissionDBO dbo = new SubmissionDBO();
		copyDtoToDbo(dto, dbo);
			
		// Set creation date
		dbo.setCreatedOn(new Date());
		
		// Ensure DBO has required information
		verifySubmissionDBO(dbo);
		
		// Create DBO
		try {
			dbo = basicDao.createNew(dbo);
		} catch (Exception e) {
			throw new DatastoreException("id=" + dbo.getId() + " userId=" + 
						dto.getUserId() + " entityId=" + dto.getEntityId(), e);
		}
	}

	public Submission get(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		SubmissionDBO dbo = basicDao.getObjectById(SubmissionDBO.class, param);
		Submission dto = new Submission();
		copyDboToDto(dbo, dto);
		return dto;
	}
	
	public List<Submission> getAllByUser(String userId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(USER_ID, userId);		
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_USER_SQL, rowMapper, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	public List<Submission> getAllByCompetition(String compId) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COMP_ID, compId);		
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_COMPETITION_SQL, rowMapper, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	public List<Submission> getAllByCompetitionAndStatus(String compId, SubmissionStatus status) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COMP_ID, compId);
		param.addValue(STATUS, status.ordinal());
		List<SubmissionDBO> dbos = simpleJdbcTemplate.query(SELECT_BY_COMPETITION_AND_STATUS_SQL, rowMapper, param);
		List<Submission> dtos = new ArrayList<Submission>();
		for (SubmissionDBO dbo : dbos) {
			Submission dto = new Submission();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}
	
	public long getCount() throws DatastoreException, NotFoundException {
		return basicDao.getCount(SubmissionDBO.class);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID, id);
		basicDao.deleteObjectById(SubmissionDBO.class, param);		
	}

	/**
	 * Copy a SubmissionDBO database object to a Participant data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	private static void copyDtoToDbo(Submission dto, SubmissionDBO dbo) {		
		dbo.setCompetitionId(dto.getCompetitionId() == null ? null : Long.parseLong(dto.getCompetitionId()));
		dbo.setUserId(dto.getCompetitionId() == null ? null : Long.parseLong(dto.getUserId()));
		dbo.setCreatedOn(dto.getCreatedOn());
	}
	
	/**
	 * Copy a Submission data transfer object to a SubmissionDBO database object
	 * 
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	private static void copyDboToDto(SubmissionDBO dbo, Submission dto) throws DatastoreException {
		dto.setId(dbo.getId() == null ? null : dbo.getId().toString());
		dto.setUserId(dbo.getUserId() == null ? null : dbo.getUserId().toString());
		dto.setCompetitionId(dbo.getCompetitionId() == null ? null : dbo.getCompetitionId().toString());
		dto.setEntityId(dbo.getEntityId() == null ? null : dbo.getEntityId().toString());
		dto.setScore(dbo.getScore());
		dto.setCreatedOn(dbo.getCreatedOn());
	}

	/**
	 * Ensure that a SubmissionDBO object has all required components
	 * 
	 * @param dbo
	 */
	private void verifySubmissionDBO(SubmissionDBO dbo) {
		Utility.ensureNotNull(dbo.getCompetitionId(), dbo.getUserId(), 
								dbo.getEntityId(), dbo.getId(), dbo.getCreatedOn()
							);
	}
	
}
