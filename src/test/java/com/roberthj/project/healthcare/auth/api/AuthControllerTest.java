package com.roberthj.project.healthcare.auth.api;

import com.roberthj.project.healthcare.member.domain.entity.Member;
import com.roberthj.project.healthcare.member.domain.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void signUp() throws Exception {
        String email = "member-" + UUID.randomUUID() + "@example.com";
        String requestEmail = "  " + email.toUpperCase() + "  ";
        String password = "Password!123";

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signUpRequest(requestEmail, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.recordKey").isNotEmpty());

        Member savedMember = memberRepository.findByEmail(email).orElseThrow();
        assertThat(savedMember.getPassword()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, savedMember.getPassword())).isTrue();
    }

    @Test
    void rejectDuplicateEmail() throws Exception {
        String email = "member-" + UUID.randomUUID() + "@example.com";
        String request = signUpRequest(email, "Password!123");

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MEMBER001"));
    }

    private String signUpRequest(String email, String password) {
        return """
            {
              "name": "홍길동",
              "nickname": "길동",
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);
    }
}
