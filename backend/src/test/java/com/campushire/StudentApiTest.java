package com.campushire;

import com.campushire.entity.Company;
import com.campushire.entity.Job;
import com.campushire.entity.JobStatus;
import com.campushire.entity.RecruiterProfile;
import com.campushire.entity.Role;
import com.campushire.entity.User;
import com.campushire.repository.CompanyRepository;
import com.campushire.repository.JobApplicationRepository;
import com.campushire.repository.JobRepository;
import com.campushire.repository.RecruiterProfileRepository;
import com.campushire.repository.UserRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class StudentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Test
    void studentCanReadAndUpdateOnlyTheirOwnProfile() throws Exception {
        String token = registerStudent("profile.student@example.com");
        mockMvc.perform(put("/api/students/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Alex Updated",
                                  "branch": "Computer Science",
                                  "cgpa": 8.75,
                                  "graduationYear": 2027,
                                  "skills": "Java, Spring, SQL",
                                  "projects": "Campus placement portal",
                                  "resumeUrl": "https://example.com/resume"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Alex Updated"))
                .andExpect(jsonPath("$.branch").value("Computer Science"))
                .andExpect(jsonPath("$.cgpa").value(8.75))
                .andExpect(jsonPath("$.email").value("profile.student@example.com"));

        mockMvc.perform(get("/api/students/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills").value("Java, Spring, SQL"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void eligibleStudentCanApplyOnlyOnceAndTrackApplication() throws Exception {
        String token = registerStudent("applicant@example.com");
        mockMvc.perform(put("/api/students/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branch": "Computer Science",
                                  "cgpa": 8.50,
                                  "skills": "Java, Spring"
                                }
                                """))
                .andExpect(status().isOk());

        Job job = createOpenJob("Software Engineer", "Computer Science", "Java", "Spring");

        mockMvc.perform(post("/api/students/jobs/{jobId}/applications", job.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.companyName").value("Acme Robotics"));

        mockMvc.perform(post("/api/students/jobs/{jobId}/applications", job.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/students/applications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPLIED"));
        assertThat(jobApplicationRepository.count()).isEqualTo(1);
    }

    @Test
    void studentWithoutRequiredCgpaBranchOrSkillsCannotApply() throws Exception {
        String token = registerStudent("ineligible@example.com");
        Job job = createOpenJob("Eligible Candidate Role", "Computer Science", "Java", "Spring");

        MvcResult result = mockMvc.perform(post("/api/students/jobs/{jobId}/applications", job.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("detail").asText())
                .contains("branch", "CGPA", "required skills");
        assertThat(jobApplicationRepository.count()).isZero();
    }

    @Test
    void studentCanOnlySeeTheirOwnInterviews() throws Exception {
        String token = registerStudent("interviews@example.com");

        mockMvc.perform(get("/api/students/interviews")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void recommendationsRankMatchingJobsAndKeepEligibilitySeparate() throws Exception {
        String token = registerStudent("recommendations@example.com");
        mockMvc.perform(put("/api/students/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "branch": "Computer Science",
                                  "cgpa": 8.50,
                                  "skills": "Java, Spring"
                                }
                                """))
                .andExpect(status().isOk());

        createOpenJob("Strong Match", "Computer Science", "Java", "Spring");
        createOpenJob("Other Requirements", "Mechanical Engineering", "Python");

        mockMvc.perform(get("/api/students/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].job.title").value("Strong Match"))
                .andExpect(jsonPath("$[0].matchScore").value(100))
                .andExpect(jsonPath("$[0].eligible").value(true))
                .andExpect(jsonPath("$[0].matchedSkills.length()").value(2))
                .andExpect(jsonPath("$[1].eligible").value(false))
                .andExpect(jsonPath("$[1].eligibilityReasons.length()").value(2));
    }

    private String registerStudent(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Campus Student",
                                  "email": "%s",
                                  "password": "student-password-123",
                                  "role": "STUDENT"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private Job createOpenJob(String title, String branch, String... requiredSkills) {
        User recruiterUser = userRepository.save(
                new User("Campus Recruiter", "recruiter-" + title.hashCode() + "@example.com",
                        "x".repeat(60), Role.RECRUITER)
        );
        RecruiterProfile recruiter = recruiterProfileRepository.save(new RecruiterProfile(recruiterUser));
        Company company = companyRepository.save(new Company(recruiter, "Acme Robotics"));
        Job job = new Job(company, title, "Build useful software", LocalDate.now().plusDays(14));
        job.setLocation("Remote");
        job.setEligibleBranch(branch);
        job.setMinimumCgpa(new BigDecimal("7.00"));
        job.setRequiredSkills(Set.of(requiredSkills));
        job.setStatus(JobStatus.APPROVED);
        return jobRepository.save(job);
    }
}
