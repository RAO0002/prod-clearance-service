package com.company.prodclearance.repository;

import com.company.prodclearance.entity.GroupProcessStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data MongoDB repository for GroupProcessStatus entity.
 * Provides CRUD operations and custom query methods.
 */
@Repository
public interface GroupProcessStatusRepository extends MongoRepository<GroupProcessStatus, String>, GroupProcessStatusCustomRepository {

    /**
     * Find all records with status 'R' (Ready) and type 'PROD'
     */
    List<GroupProcessStatus> findByStatusAndType(String status, String type);

    /**
     * Count records by status
     */
    long countByStatus(String status);

    /**
     * Find by AWS Request ID for traceability
     */
    GroupProcessStatus findByAwsRequestId(String awsRequestId);
}
