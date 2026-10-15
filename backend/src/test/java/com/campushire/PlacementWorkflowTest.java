package com.campushire;

import com.campushire.dto.ApplicationStatusRequest;
import com.campushire.dto.InterviewRequest;
import com.campushire.dto.JobRequest;
import com.campushire.dto.RegisterRequest;
import com.campushire.dto.StudentProfileRequest;
import com.campushire.entity.ApplicationStatus;
import com.campushire.entity.Role;
import com.campushire.repository.ApplicationStatusHistoryRepository;
import com.campushire.repository.JobApplicationRepository;
import com.campushire.repository.NotificationRepository;
import com.campushire.repository.PlacementRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:campushire-workflow;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.jwt.expiration-ms=900000",
        "app.bootstrap.officer-email=officer.test@campushire.edu",
        "app.bootstrap.officer-password=StrongOfficerPass123"
})
@AutoConfigureMockMvc
@Transactional
class PlacementWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private PlacementRecordRepository placementRecordRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void completeLifecycleFromApplicationToPlacementAndAnalytics() throws Exception {
        // 1. Officer Login
        String officerToken = login("officer.test@campushire.edu", "StrongOfficerPass123");

        // 2. Student Register & Update Profile
        String studentToken = register("student.flow@test.edu", "StudentPass123", Role.STUDENT);
        StudentProfileRequest profileReq = new StudentProfileRequest(
                "Flow Student",
                "STU-2026-001",
                "9876543210",
                "Computer Science",
                "B.Tech",
                new BigDecimal("8.80"),
                2026,
                "Java, Spring, SQL, React",
                "Full Stack, Cloud",
                "Placement Portal Project",
                "https://example.com/resume.pdf"
        );
        mockMvc.perform(put("/api/students/profile")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileReq)))
                .andExpect(status().isOk());

        // 3. Recruiter Register & Create Company & Post Job
        String recruiterToken = register("recruiter.flow@test.edu", "RecruiterPass123", Role.RECRUITER);
        MvcResult compResult = mockMvc.perform(post("/api/recruiters/companies")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Apex Innovations",
                                  "description": "Tech enterprise",
                                  "website": "https://apex.example.com",
                                  "headquarters": "Hyderabad"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        Long companyId = objectMapper.readTree(compResult.getResponse().getContentAsString()).get("id").asLong();

        JobRequest jobReq = new JobRequest(
                companyId,
                "Software Development Engineer",
                "Build scalable cloud-native backend services and web applications",
                "Bengaluru",
                "FULL_TIME",
                new BigDecimal("1200000.00"),
                "Computer Science",
                "B.Tech",
                new BigDecimal("7.50"),
                Set.of("Java", "Spring"),
                LocalDate.now().plusDays(30)
        );
        MvcResult jobResult = mockMvc.perform(post("/api/recruiters/jobs")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isOk())
                .andReturn();
        Long jobId = objectMapper.readTree(jobResult.getResponse().getContentAsString()).get("id").asLong();

        // 4. Officer Approves Job
        mockMvc.perform(post("/api/officer/jobs/{jobId}/approve", jobId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\": \"Approved for 2026 drive\"}"))
                .andExpect(status().isOk());

        // 5. Student Views Recommendations and Applies
        mockMvc.perform(get("/api/students/recommendations")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].job.title").value("Software Development Engineer"))
                .andExpect(jsonPath("$[0].eligible").value(true))
                .andExpect(jsonPath("$[0].matchScore").value(100));

        MvcResult applyResult = mockMvc.perform(post("/api/students/jobs/{jobId}/applications", jobId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andReturn();
        Long applicationId = objectMapper.readTree(applyResult.getResponse().getContentAsString()).get("id").asLong();

        // Verify initial status is APPLIED and history is recorded
        var histories = statusHistoryRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getToStatus()).isEqualTo(ApplicationStatus.APPLIED);

        // 6. Recruiter Transitions: APPLIED -> UNDER_REVIEW -> SHORTLISTED
        mockMvc.perform(patch("/api/recruiters/applications/{appId}/status", applicationId)
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApplicationStatusRequest(ApplicationStatus.UNDER_REVIEW, "Profile being reviewed"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/recruiters/applications/{appId}/status", applicationId)
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApplicationStatusRequest(ApplicationStatus.SHORTLISTED, "Candidate shortlisted for technical round"))))
                .andExpect(status().isOk());

        // 7. Recruiter Schedules Interview
        InterviewRequest interviewReq = new InterviewRequest(
                Instant.now().plusSeconds(86400),
                "TECHNICAL",
                "ONLINE",
                "Technical Round 1",
                "https://meet.google.com/xyz-abc-def",
                "SCHEDULED",
                "Prepare Data Structures & Spring Boot fundamentals"
        );
        mockMvc.perform(post("/api/recruiters/applications/{appId}/interviews", applicationId)
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(interviewReq)))
                .andExpect(status().isOk());

        // Verify application status changed to INTERVIEW_SCHEDULED
        var app = applicationRepository.findById(applicationId).orElseThrow();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW_SCHEDULED);

        // 8. Recruiter Marks Candidate as SELECTED
        mockMvc.perform(patch("/api/recruiters/applications/{appId}/status", applicationId)
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApplicationStatusRequest(ApplicationStatus.SELECTED, "Offered role with 12 LPA CTC"))))
                .andExpect(status().isOk());

        // 9. Verify Placement Record is persisted in DB!
        assertThat(placementRecordRepository.count()).isEqualTo(1);
        var placement = placementRecordRepository.findAll().get(0);
        assertThat(placement.getPackageCtc()).isEqualByComparingTo(new BigDecimal("1200000.00"));
        assertThat(placement.getPlacementStatus()).isEqualTo("OFFERED");
        assertThat(placement.getJob().getTitle()).isEqualTo("Software Development Engineer");

        // 10. Student Views Their Placements & Application History
        mockMvc.perform(get("/api/students/placements")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyName").value("Apex Innovations"))
                .andExpect(jsonPath("$[0].packageCtc").value(1200000.00));

        mockMvc.perform(get("/api/students/applications/{appId}/history", applicationId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5)); // APPLIED -> UNDER_REVIEW -> SHORTLISTED -> INTERVIEW_SCHEDULED -> SELECTED

        // 11. Student Checks Notifications
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(5)); // applied, under review, shortlisted, interview, selected

        // 12. Officer Views Analytics & Dashboard
        mockMvc.perform(get("/api/officer/analytics")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOffers").value(1))
                .andExpect(jsonPath("$.highestPackage").value(1200000.00));

        mockMvc.perform(get("/api/officer/dashboard")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedApplications").value(1))
                .andExpect(jsonPath("$.placedStudents").value(1))
                .andExpect(jsonPath("$.placementPercentage").value(100.0));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String register(String email, String password, Role role) throws Exception {
        RegisterRequest req = new RegisterRequest("Test User", email, password, role);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
