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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "interviews")
public class Interview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheduled_by_user_id", nullable = false)
    private User scheduledBy;

    @NotNull
    @Column(nullable = false)
    private Instant scheduledAt;

    @Size(max = 50)
    @Column(length = 50)
    private String interviewType;

    @Size(max = 50)
    @Column(length = 50)
    private String interviewMode = "ONLINE";

    @Size(max = 100)
    @Column(length = 100)
    private String round = "Technical Round";

    @Size(max = 500)
    @Column(length = 500)
    private String locationOrMeetingUrl;

    @Size(max = 30)
    @Column(length = 30)
    private String status = "SCHEDULED";

    @Size(max = 2000)
    @Column(length = 2000)
    private String notes;

    protected Interview() {
    }

    public Interview(JobApplication application, User scheduledBy, Instant scheduledAt) {
        this.application = application;
        this.scheduledBy = scheduledBy;
        this.scheduledAt = scheduledAt;
    }

    public Long getId() {
        return id;
    }

    public JobApplication getApplication() {
        return application;
    }

    public User getScheduledBy() {
        return scheduledBy;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getInterviewType() {
        return interviewType;
    }

    public void setInterviewType(String interviewType) {
        this.interviewType = interviewType;
    }

    public String getInterviewMode() {
        return interviewMode;
    }

    public void setInterviewMode(String interviewMode) {
        this.interviewMode = interviewMode;
    }

    public String getRound() {
        return round;
    }

    public void setRound(String round) {
        this.round = round;
    }

    public String getLocationOrMeetingUrl() {
        return locationOrMeetingUrl;
    }

    public void setLocationOrMeetingUrl(String locationOrMeetingUrl) {
        this.locationOrMeetingUrl = locationOrMeetingUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
