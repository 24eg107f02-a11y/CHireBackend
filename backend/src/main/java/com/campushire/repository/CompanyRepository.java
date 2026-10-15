package com.campushire.repository;

import com.campushire.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByRecruiterIdOrderByNameAsc(Long recruiterId);

    @Query("""
            select c from Company c
            join fetch c.recruiter r
            where r.user.id = :userId
            order by c.name asc
            """)
    List<Company> findAllForRecruiter(@Param("userId") Long userId);

    @Query("""
            select c from Company c
            join fetch c.recruiter r
            where c.id = :companyId and r.user.id = :userId
            """)
    Optional<Company> findOwnedCompany(@Param("companyId") Long companyId, @Param("userId") Long userId);

    boolean existsByIdAndRecruiterUserId(Long companyId, Long userId);
}
