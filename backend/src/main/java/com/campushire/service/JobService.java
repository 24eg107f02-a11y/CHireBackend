package com.campushire.service;

import com.campushire.dto.JobResponse;
import com.campushire.entity.JobStatus;
import com.campushire.repository.JobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public List<JobResponse> findOpenJobs(String company, String location, String branch, BigDecimal cgpa) {
        return jobRepository.findOpenJobs(
                        JobStatus.APPROVED,
                        LocalDate.now(),
                        cleanFilter(company),
                        cleanFilter(location),
                        cleanFilter(branch),
                        cgpa
                ).stream()
                .map(JobResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobResponse findOpenJob(Long id) {
        return jobRepository.findOpenJobById(id, JobStatus.APPROVED, LocalDate.now())
                .map(JobResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Open job not found"));
    }

    private String cleanFilter(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
