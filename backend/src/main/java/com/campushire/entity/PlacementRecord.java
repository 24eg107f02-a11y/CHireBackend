package com.campushire.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "placement_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_placement_student_job", columnNames = {"student_profile_id", "job_id"})
        }
)
public class PlacementRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @DecimalMin("0.00")
    @Column(precision = 12, scale = 2)
    private BigDecimal packageCtc;

    @NotNull
    @Column(nullable = false)
    private LocalDate selectionDate;

    @Size(max = 50)
    @Column(length = 50)
    private String placementStatus = "OFFERED";

    protected PlacementRecord() {
    }

    public PlacementRecord(StudentProfile student, Company company, Job job, BigDecimal packageCtc, LocalDate selectionDate) {
        this.student = student;
        this.company = company;
        this.job = job;
        this.packageCtc = packageCtc;
        this.selectionDate = selectionDate;
    }

    public Long getId() {
        return id;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public Company getCompany() {
        return company;
    }

    public Job getJob() {
        return job;
    }

    public BigDecimal getPackageCtc() {
        return packageCtc;
    }

    public void setPackageCtc(BigDecimal packageCtc) {
        this.packageCtc = packageCtc;
    }

    public LocalDate getSelectionDate() {
        return selectionDate;
    }

    public void setSelectionDate(LocalDate selectionDate) {
        this.selectionDate = selectionDate;
    }

    public String getPlacementStatus() {
        return placementStatus;
    }

    public void setPlacementStatus(String placementStatus) {
        this.placementStatus = placementStatus;
    }
}
