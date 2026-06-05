package com.company.prodclearance.repository;

import com.company.prodclearance.model.GroupProcessStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for GroupProcessStatus documents.
 */
@Repository
public interface GroupProcessStatusRepository extends MongoRepository<GroupProcessStatus, String> {

    /**
     * Find all records with 'R' (Received) status.
     */
    @Query("{ 'status': 'R' }")
    List<GroupProcessStatus> findAllReceivedRecords();

    /**
     * Find records with status 'R' limit by batch size.
     */
    @Query("{ 'status': 'R' }")
    List<GroupProcessStatus> findReceivedRecordsByLimit(int limit);

    /**
     * Count records with specific status.
     */
    long countByStatus(String status);

}