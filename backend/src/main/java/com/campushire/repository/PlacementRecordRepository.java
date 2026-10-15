package com.campushire.repository;

import com.campushire.entity.PlacementRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PlacementRecordRepository extends JpaRepository<PlacementRecord, Long> {

    Optional<PlacementRecord> findByStudentIdAndJobId(Long studentId, Long jobId);

    @Query("""
            select p from PlacementRecord p
            join fetch p.student s
            join fetch s.user u
            join fetch p.company c
            join fetch p.job j
            order by p.selectionDate desc, p.createdAt desc
            """)
    List<PlacementRecord> findAllWithDetails();

    @Query("""
            select p from PlacementRecord p
            join fetch p.student s
            join fetch p.company c
            join fetch p.job j
            where s.user.id = :userId
            order by p.selectionDate desc
            """)
    List<PlacementRecord> findByUserId(@Param("userId") Long userId);

    @Query("select count(distinct p.student.id) from PlacementRecord p")
    long countDistinctPlacedStudents();

    @Query("select coalesce(max(p.packageCtc), 0.0) from PlacementRecord p")
    BigDecimal findHighestPackage();

    @Query("select coalesce(avg(p.packageCtc), 0.0) from PlacementRecord p")
    BigDecimal findAveragePackage();

    @Query("""
            select p.company.name, count(p)
            from PlacementRecord p
            group by p.company.name
            order by count(p) desc
            """)
    List<Object[]> findPlacementsByCompany();

    @Query("""
            select coalesce(p.student.branch, 'Unspecified'), count(p)
            from PlacementRecord p
            group by p.student.branch
            order by count(p) desc
            """)
    List<Object[]> findPlacementsByBranch();
}
