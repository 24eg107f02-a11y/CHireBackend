package com.campushire;

import com.campushire.entity.Role;
import com.campushire.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class AuthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void studentCanRegisterLoginAndReadTheirIdentityWithoutPasswordFields() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Alex Student",
                                  "email": "Alex.Student@example.com",
                                  "password": "student-password-123",
                                  "role": "STUDENT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode registrationBody = objectMapper.readTree(registration.getResponse().getContentAsString());
        String token = registrationBody.get("accessToken").asText();
        assertThat(registrationBody.get("role").asText()).isEqualTo(Role.STUDENT.name());
        assertThat(registrationBody.has("password")).isFalse();
        assertThat(registrationBody.has("passwordHash")).isFalse();

        var savedUser = userRepository.findByEmailIgnoreCase("alex.student@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("student-password-123", savedUser.getPasswordHash())).isTrue();

        MvcResult identity = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode identityBody = objectMapper.readTree(identity.getResponse().getContentAsString());
        assertThat(identityBody.get("email").asText()).isEqualTo("alex.student@example.com");
        assertThat(identityBody.has("accessToken")).isFalse();
        assertThat(identityBody.has("passwordHash")).isFalse();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ALEX.STUDENT@example.com",
                                  "password": "student-password-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"tokenType":"Bearer","role":"STUDENT","email":"alex.student@example.com"}
                        """, false));
    }

    @Test
    void publicRegistrationCannotCreatePlacementOfficer() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Untrusted Officer",
                                  "email": "officer@example.com",
                                  "password": "long-enough-password",
                                  "role": "PLACEMENT_OFFICER"
                                }
                                """))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsByEmailIgnoreCase("officer@example.com")).isFalse();
    }

    @Test
    void loginRejectsIncorrectPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Recruiter User",
                                  "email": "recruiter@example.com",
                                  "password": "correct-password-123",
                                  "role": "RECRUITER"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "recruiter@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void recruiterTokenCannotAccessStudentRoutes() throws Exception {
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Recruiter User",
                                  "email": "role-check@example.com",
                                  "password": "correct-password-123",
                                  "role": "RECRUITER"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(registration.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/students/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void protectedApiRejectsMissingOrInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }
}
