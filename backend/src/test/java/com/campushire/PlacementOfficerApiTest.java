package com.campushire;

import com.campushire.entity.Job;
import com.campushire.entity.JobStatus;
import com.campushire.repository.JobRepository;
import com.campushire.repository.PlacementDriveRepository;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        "app.jwt.expiration-ms=900000",
        "app.bootstrap.officer-email=placement.officer@example.com",
        "app.bootstrap.officer-password=officer-test-password-123"
})
@AutoConfigureMockMvc
@Transactional
class PlacementOfficerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlacementDriveRepository placementDriveRepository;

    @Test
    void officerCanApproveRejectAndListJobsByStatus() throws Exception {
        String officerToken = loginOfficer();
        String recruiterToken = registerRecruiter();
        long companyId = createCompany(recruiterToken);
        long jobId = createJob(recruiterToken, companyId, "Approved Engineer");

        mockMvc.perform(get("/api/officer/jobs")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].job.id").value(jobId))
                .andExpect(jsonPath("$[0].job.status").value("PENDING_APPROVAL"));

        mockMvc.perform(post("/api/officer/jobs/{jobId}/approve", jobId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notes":"Eligibility criteria reviewed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewNotes").value("Eligibility criteria reviewed"));

        mockMvc.perform(post("/api/officer/jobs/{jobId}/reject", jobId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isConflict());

        long secondJobId = createJob(recruiterToken, companyId, "Rejected Engineer");
        mockMvc.perform(post("/api/officer/jobs/{jobId}/reject", secondJobId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.status").value("REJECTED"));

        mockMvc.perform(get("/api/officer/jobs")
                        .param("status", "REJECTED")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].job.id").value(secondJobId));
    }

    @Test
    void officerCanManageDrivesAndAssignJobs() throws Exception {
        String officerToken = loginOfficer();
        String recruiterToken = registerRecruiter();
        long companyId = createCompany(recruiterToken);
        long jobId = createJob(recruiterToken, companyId, "Drive Engineer");
        long driveId = createDrive(officerToken);

        mockMvc.perform(put("/api/officer/jobs/{jobId}/placement-drive", jobId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"placementDriveId": %d}
                                """.formatted(driveId)))
                .andExpect(status().isOk());

        Job job = jobRepository.findById(jobId).orElseThrow();
        assertThat(job.getPlacementDrive().getId()).isEqualTo(driveId);

        mockMvc.perform(delete("/api/officer/drives/{driveId}", driveId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/officer/jobs/{jobId}/placement-drive", jobId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"placementDriveId":null}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/officer/drives/{driveId}", driveId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driveRequest("Updated Drive", LocalDate.now().plusDays(3),
                                LocalDate.now().plusDays(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Drive"));

        mockMvc.perform(delete("/api/officer/drives/{driveId}", driveId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isNoContent());
        assertThat(placementDriveRepository.existsById(driveId)).isFalse();
    }

    @Test
    void officerCanViewDashboardAndPlacementReport() throws Exception {
        String officerToken = loginOfficer();
        mockMvc.perform(get("/api/officer/dashboard")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placementOfficers").value(1))
                .andExpect(jsonPath("$.placementDrives").value(0))
                .andExpect(jsonPath("$.selectedApplications").value(0));

        mockMvc.perform(get("/api/officer/reports/placements")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedStudents").value(0))
                .andExpect(jsonPath("$.placementsByCompany").isArray());
    }

    @Test
    void officerCanDisableUsersButCannotDisableSelfOrLastEnabledOfficer() throws Exception {
        String officerToken = loginOfficer();
        String recruiterToken = registerRecruiter();
        JsonNode recruiter = identity(recruiterToken);
        long recruiterId = recruiter.get("id").asLong();

        mockMvc.perform(patch("/api/officer/users/{userId}/status", recruiterId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/recruiters/companies")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isUnauthorized());

        JsonNode officer = identity(officerToken);
        mockMvc.perform(patch("/api/officer/users/{userId}/status", officer.get("id").asLong())
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidDriveDateRangeIsRejected() throws Exception {
        String officerToken = loginOfficer();
        mockMvc.perform(post("/api/officer/drives")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driveRequest("Invalid Drive", LocalDate.now().plusDays(5),
                                LocalDate.now().plusDays(2))))
                .andExpect(status().isBadRequest());
    }

    private String loginOfficer() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"placement.officer@example.com",
                                  "password":"officer-test-password-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String registerRecruiter() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Campus Recruiter",
                                  "email":"%s@example.com",
                                  "password":"recruiter-password-123",
                                  "role":"RECRUITER"
                                }
                                """.formatted(java.util.UUID.randomUUID())))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private JsonNode identity(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long createCompany(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recruiters/companies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Officer Test Company"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createJob(String token, long companyId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/recruiters/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId":%d,
                                  "title":"%s",
                                  "description":"An officer API test role",
                                  "applicationDeadline":"%s"
                                }
                                """.formatted(companyId, title, LocalDate.now().plusDays(14))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createDrive(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/officer/drives")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(driveRequest("Campus Placement Week", LocalDate.now().plusDays(3),
                                LocalDate.now().plusDays(5))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String driveRequest(String name, LocalDate start, LocalDate end) {
        return """
                {
                  "name":"%s",
                  "description":"Placement drive integration test",
                  "startDate":"%s",
                  "endDate":"%s",
                  "venue":"Campus auditorium"
                }
                """.formatted(name, start, end);
    }
}
