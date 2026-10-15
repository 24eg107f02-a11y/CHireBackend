package com.campushire;

import com.campushire.entity.Job;
import com.campushire.repository.JobApplicationRepository;
import com.campushire.repository.JobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:campushire-auth;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.jwt.expiration-ms=900000"
})
@AutoConfigureMockMvc
@Transactional
class RecruiterApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Test
    void recruiterCanManageCompanyJobApplicantsAndScheduleInterview() throws Exception {
        String recruiterToken = register("recruiter", "RECRUITER");
        long companyId = createCompany(recruiterToken);
        long jobId = createJob(recruiterToken, companyId, "Backend Engineer");
        Job job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(com.campushire.entity.JobStatus.APPROVED);

        String studentToken = register("student", "STUDENT");
        mockMvc.perform(put("/api/students/profile")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branch":"Computer Science","cgpa":8.5,"skills":"Java, Spring"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/students/jobs/{jobId}/applications", jobId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        MvcResult applicants = mockMvc.perform(get("/api/recruiters/jobs/{jobId}/applications", jobId)
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Campus student"))
                .andExpect(jsonPath("$[0].email").value("student@example.com"))
                .andExpect(jsonPath("$[0].skills").value("Java, Spring"))
                .andReturn();
        long applicationId = objectMapper.readTree(applicants.getResponse().getContentAsString())
                .get(0).get("applicationId").asLong();

        mockMvc.perform(patch("/api/recruiters/applications/{applicationId}/status", applicationId)
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SHORTLISTED","recruiterNotes":"Schedule a technical interview"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));

        String scheduledTime = Instant.now().plusSeconds(3600).toString();
        mockMvc.perform(post("/api/recruiters/applications/{applicationId}/interviews", applicationId)
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scheduledAt": "%s",
                                  "interviewType": "TECHNICAL",
                                  "locationOrMeetingUrl": "https://meet.example.com/room",
                                  "notes": "Bring a laptop"
                                }
                                """.formatted(scheduledTime)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationId").value(applicationId))
                .andExpect(jsonPath("$.interviewType").value("TECHNICAL"));

        mockMvc.perform(get("/api/students/interviews")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(patch("/api/recruiters/applications/{applicationId}/status", applicationId)
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SELECTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SELECTED"));

        mockMvc.perform(patch("/api/recruiters/applications/{applicationId}/status", applicationId)
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"APPLIED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recruiterCannotAccessOrChangeAnotherRecruitersResources() throws Exception {
        String ownerToken = register("owner", "RECRUITER");
        String otherToken = register("other", "RECRUITER");
        long companyId = createCompany(ownerToken);
        long jobId = createJob(ownerToken, companyId, "Owned Engineer");

        mockMvc.perform(get("/api/recruiters/companies")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(put("/api/recruiters/jobs/{jobId}", jobId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobRequest(companyId, "Stolen Job")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/recruiters/jobs/{jobId}/applications", jobId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void recruiterJobChangesRequireReapprovalAndJobsCanBeClosed() throws Exception {
        String token = register("approval", "RECRUITER");
        long companyId = createCompany(token);
        long jobId = createJob(token, companyId, "First Title");
        Job job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(com.campushire.entity.JobStatus.APPROVED);

        mockMvc.perform(put("/api/recruiters/jobs/{jobId}", jobId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobRequest(companyId, "Updated Title")))
                .andExpect(status().isOk());
        assertThat(jobRepository.findById(jobId).orElseThrow().getStatus())
                .isEqualTo(com.campushire.entity.JobStatus.PENDING_APPROVAL);

        mockMvc.perform(post("/api/recruiters/jobs/{jobId}/close", jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(jobRepository.findById(jobId).orElseThrow().getStatus())
                .isEqualTo(com.campushire.entity.JobStatus.CLOSED);
        assertThat(jobApplicationRepository.count()).isZero();
    }

    @Test
    void companyWithJobCannotBeDeleted() throws Exception {
        String token = register("delete-company", "RECRUITER");
        long companyId = createCompany(token);
        createJob(token, companyId, "Company Job");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/recruiters/companies/{companyId}", companyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    private String register(String suffix, String role) throws Exception {
        String email = suffix + "@example.com";
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Campus %s",
                                  "email": "%s",
                                  "password": "recruiter-password-123",
                                  "role": "%s"
                                }
                                """.formatted(suffix, email, role)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private long createCompany(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recruiters/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Example Technologies",
                                  "description": "Software company",
                                  "website": "https://example.com",
                                  "headquarters": "Bengaluru"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createJob(String token, long companyId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recruiters/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobRequest(companyId, title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyId").value(companyId))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String jobRequest(long companyId, String title) {
        return """
                {
                  "companyId": %d,
                  "title": "%s",
                  "description": "Build software for customers",
                  "location": "Remote",
                  "employmentType": "FULL_TIME",
                  "salary": 1200000,
                  "eligibleBranch": "Computer Science",
                  "minimumCgpa": 7.0,
                  "requiredSkills": ["Java", "Spring"],
                  "applicationDeadline": "%s"
                }
                """.formatted(companyId, title, LocalDate.now().plusDays(14));
    }
}
