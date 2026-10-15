package com.campushire.repository;

import com.campushire.entity.JobApplication;
import com.campushire.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByJobIdAndStudentId(Long jobId, Long studentId);

    boolean existsByJobId(Long jobId);

    List<JobApplication> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<JobApplication> findByJobIdOrderByCreatedAtDesc(Long jobId);

    @Query("""
            select a from JobApplication a
            join fetch a.student s
            join fetch s.user u
            join fetch a.job j
            join fetch j.company c
            order by a.createdAt desc
            """)
    List<JobApplication> findAllWithDetails();

    @Query("""
            select a from JobApplication a
            join fetch a.student s
            join fetch s.user
            join fetch a.job j
            join fetch j.company c
            join c.recruiter r
            where j.id = :jobId and r.user.id = :userId
            order by a.createdAt desc
            """)
    List<JobApplication> findApplicantsForOwnedJob(
            @Param("jobId") Long jobId,
            @Param("userId") Long userId
    );

    @Query("""
            select a from JobApplication a
            join fetch a.job j
            join j.company c
            join c.recruiter r
            where a.id = :applicationId and r.user.id = :userId
            """)
    Optional<JobApplication> findOwnedApplication(
            @Param("applicationId") Long applicationId,
            @Param("userId") Long userId
    );

    boolean existsByJobCompanyId(Long companyId);

    long countByStatus(ApplicationStatus status);

    @Query("""
            select new com.campushire.dto.PlacementReportRow(c.id, c.name, count(distinct a.student.id))
            from JobApplication a
            join a.job j
            join j.company c
            where a.status = :status
            group by c.id, c.name
            order by count(distinct a.student.id) desc, c.name asc
            """)
    List<com.campushire.dto.PlacementReportRow> findPlacementCountsByCompany(
            @Param("status") ApplicationStatus status
    );
}
