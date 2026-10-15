package com.campushire.repository;

import com.campushire.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplicationStudentIdOrderByScheduledAtAsc(Long studentId);

    List<Interview> findByApplicationJobIdOrderByScheduledAtAsc(Long jobId);

    List<Interview> findAllByOrderByScheduledAtDesc();

    @Query("""
            select i from Interview i
            join fetch i.application a
            join fetch a.student s
            join fetch s.user
            join fetch a.job j
            join fetch j.company c
            join c.recruiter r
            where i.id = :interviewId and r.user.id = :userId
            """)
    Optional<Interview> findOwnedInterview(
            @Param("interviewId") Long interviewId,
            @Param("userId") Long userId
    );
}
