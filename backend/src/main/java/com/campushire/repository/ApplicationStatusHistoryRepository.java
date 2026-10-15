package com.campushire.repository;

import com.campushire.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {

    @Query("""
            select h from ApplicationStatusHistory h
            left join fetch h.changedBy
            where h.application.id = :applicationId
            order by h.createdAt asc
            """)
    List<ApplicationStatusHistory> findByApplicationIdOrderByCreatedAtAsc(@Param("applicationId") Long applicationId);
}
