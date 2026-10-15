package com.campushire.repository;

import com.campushire.entity.Job;
import com.campushire.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("""
            select j from Job j
            join fetch j.company c
            where j.status = :status
              and j.applicationDeadline >= :today
              and (:company is null or lower(c.name) like lower(concat('%', :company, '%')))
              and (:location is null or lower(j.location) like lower(concat('%', :location, '%')))
              and (:branch is null or j.eligibleBranch is null
                   or lower(j.eligibleBranch) = lower(:branch))
              and (:cgpa is null or j.minimumCgpa is null or j.minimumCgpa <= :cgpa)
            order by j.applicationDeadline asc
            """)
    List<Job> findOpenJobs(@Param("status") JobStatus status,
                           @Param("today") LocalDate today,
                           @Param("company") String company,
                           @Param("location") String location,
                           @Param("branch") String branch,
                           @Param("cgpa") BigDecimal cgpa);

    @Query("""
            select j from Job j
            join fetch j.company
            where j.id = :id and j.status = :status and j.applicationDeadline >= :today
            """)
    Optional<Job> findOpenJobById(@Param("id") Long id,
                                  @Param("status") JobStatus status,
                                  @Param("today") LocalDate today);

    @Query("""
            select j from Job j
            join fetch j.company c
            join c.recruiter r
            where r.user.id = :userId
            order by j.createdAt desc
            """)
    List<Job> findAllForRecruiter(@Param("userId") Long userId);

    @Query("""
            select j from Job j
            join fetch j.company c
            join c.recruiter r
            where j.id = :jobId and r.user.id = :userId
            """)
    Optional<Job> findOwnedJob(@Param("jobId") Long jobId, @Param("userId") Long userId);

    @Query("""
            select count(j) > 0 from Job j
            join j.company c
            join c.recruiter r
            where c.id = :companyId and r.user.id = :userId
            """)
    boolean existsByCompanyAndRecruiterUser(@Param("companyId") Long companyId, @Param("userId") Long userId);

    boolean existsByCompanyId(Long companyId);

    long countByStatus(com.campushire.entity.JobStatus status);

    List<Job> findByStatusOrderByCreatedAtAsc(com.campushire.entity.JobStatus status);

    boolean existsByPlacementDriveId(Long placementDriveId);
}
