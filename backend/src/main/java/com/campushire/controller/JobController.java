package com.campushire.controller;

import com.campushire.dto.JobResponse;
import com.campushire.service.JobService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public List<JobResponse> findJobs(
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) @DecimalMin("0.00") @DecimalMax("10.00") BigDecimal cgpa
    ) {
        return jobService.findOpenJobs(company, location, branch, cgpa);
    }

    @GetMapping("/{id}")
    public JobResponse findJob(@PathVariable Long id) {
        return jobService.findOpenJob(id);
    }
}
