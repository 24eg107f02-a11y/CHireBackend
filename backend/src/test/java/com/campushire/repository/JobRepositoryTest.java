package com.campushire.repository;

import com.campushire.entity.Company;
import com.campushire.entity.Job;
import com.campushire.entity.JobStatus;
import com.campushire.entity.RecruiterProfile;
import com.campushire.entity.Role;
import com.campushire.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JobRepositoryTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void findOpenJobsReturnsOnlyApprovedUnexpiredJobsMatchingFilters() {
        User recruiterUser = userRepository.save(
                new User("Campus Recruiter", "recruiter@example.com", "x".repeat(60), Role.RECRUITER)
        );
        RecruiterProfile recruiter = recruiterProfileRepository.save(new RecruiterProfile(recruiterUser));
        Company company = companyRepository.save(new Company(recruiter, "Acme Robotics"));

        Job matchingJob = new Job(
                company,
                "Graduate Software Engineer",
                "Build placement portal features",
                LocalDate.now().plusDays(14)
        );
        matchingJob.setLocation("Remote");
        matchingJob.setEligibleBranch("Computer Science");
        matchingJob.setMinimumCgpa(new BigDecimal("7.50"));
        matchingJob.setStatus(JobStatus.APPROVED);
        jobRepository.save(matchingJob);

        Job unapprovedJob = new Job(
                company,
                "Pending Engineer",
                "This role is awaiting placement officer approval",
                LocalDate.now().plusDays(14)
        );
        jobRepository.save(unapprovedJob);

        Job expiredJob = new Job(
                company,
                "Expired Engineer",
                "This role is no longer open",
                LocalDate.now().minusDays(1)
        );
        expiredJob.setStatus(JobStatus.APPROVED);
        jobRepository.save(expiredJob);

        List<Job> results = jobRepository.findOpenJobs(
                JobStatus.APPROVED,
                LocalDate.now(),
                "acme",
                "remote",
                "computer science",
                new BigDecimal("8.00")
        );

        assertThat(results).extracting(Job::getTitle)
                .containsExactly("Graduate Software Engineer");
    }

    @Test
    void findOpenJobsExcludesJobsAboveCandidateCgpa() {
        User recruiterUser = userRepository.save(
                new User("Campus Recruiter", "second.recruiter@example.com", "x".repeat(60), Role.RECRUITER)
        );
        RecruiterProfile recruiter = recruiterProfileRepository.save(new RecruiterProfile(recruiterUser));
        Company company = companyRepository.save(new Company(recruiter, "Northwind"));
        Job job = new Job(company, "Senior Graduate Engineer", "Develop software", LocalDate.now().plusDays(7));
        job.setMinimumCgpa(new BigDecimal("8.50"));
        job.setStatus(JobStatus.APPROVED);
        jobRepository.save(job);

        List<Job> results = jobRepository.findOpenJobs(
                JobStatus.APPROVED,
                LocalDate.now(),
                null,
                null,
                null,
                new BigDecimal("8.00")
        );

        assertThat(results).isEmpty();
    }
}
