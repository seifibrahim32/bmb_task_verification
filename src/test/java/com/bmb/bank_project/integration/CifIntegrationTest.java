package com.bmb.bank_project.integration;

import com.bmb.bank_project.dto.AuthenticateRequest;
import com.bmb.bank_project.dto.RegisterRequest;
import com.bmb.bank_project.dto.ResetTpinRequest;
import com.bmb.bank_project.dto.SetTpinRequest;
import com.bmb.bank_project.model.CifStatus;
import com.bmb.bank_project.repository.CifRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CIF API Integration Tests")
class CifIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper is not exposed as an injectable bean in Spring Boot 4's
    // test slice — instantiate directly (same fix applied to CifControllerTest).
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CifRepository cifRepository;

    private RegisterRequest registerReq(String email) {
        RegisterRequest r = new RegisterRequest();
        r.setFullName("John Doe");
        r.setEmail(email);
        r.setPhoneNumber("+9665XXXXXXXX");
        return r;
    }

    private SetTpinRequest setTpinReq(String cifNumber, String tpin) {
        SetTpinRequest r = new SetTpinRequest();
        r.setCifNumber(cifNumber);
        r.setTpin(tpin);
        r.setConfirmTpin(tpin);
        return r;
    }

    private AuthenticateRequest authReq(String cifNumber, String tpin) {
        AuthenticateRequest r = new AuthenticateRequest();
        r.setCifNumber(cifNumber);
        r.setTpin(tpin);
        return r;
    }

    private ResetTpinRequest resetReq(String cifNumber, String newTpin) {
        ResetTpinRequest r = new ResetTpinRequest();
        r.setCifNumber(cifNumber);
        r.setNewTpin(newTpin);
        r.setConfirmTpin(newTpin);
        return r;
    }

    private String registerAndGetCifNumber(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/cif/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq(email))))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("cifNumber").asText();
    }

    @Test
    @DisplayName("Happy path – register → setTpin → authenticate successfully")
    void fullFlow_registerSetTpinAuthenticate() throws Exception {
        String cifNumber = registerAndGetCifNumber("happy@example.com");
        assertThat(cifNumber).startsWith("CIF-");

        mockMvc.perform(post("/api/cif/set-tpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setTpinReq(cifNumber, "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(cifRepository.findByCifNumber(cifNumber))
                .hasValueSatisfying(c -> assertThat(c.getStatus()).isEqualTo(CifStatus.ACTIVE));

        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(cifNumber, "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true));
    }

    @Test
    @DisplayName("Block flow – 3 wrong TPINs block the CIF")
    void fullFlow_threeFailedAttempts_blocksCif() throws Exception {
        String cifNumber = registerAndGetCifNumber("block@example.com");

        mockMvc.perform(post("/api/cif/set-tpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setTpinReq(cifNumber, "1234"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(cifNumber, "0000"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.remainingAttempts").value(2));

        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(cifNumber, "0000"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.remainingAttempts").value(1));

        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(cifNumber, "0000"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("blocked")));

        assertThat(cifRepository.findByCifNumber(cifNumber))
                .hasValueSatisfying(c -> {
                    assertThat(c.getStatus()).isEqualTo(CifStatus.BLOCKED);
                    assertThat(c.getFailedAttempts()).isEqualTo(3);
                });
    }

    @Test
    @DisplayName("Reset flow – unblock blocked CIF and authenticate with new TPIN")
    void fullFlow_resetTpin_unblockAndReauthenticate() throws Exception {
        String cifNumber = registerAndGetCifNumber("reset@example.com");

        mockMvc.perform(post("/api/cif/set-tpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setTpinReq(cifNumber, "1234"))))
                .andExpect(status().isOk());

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/cif/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(authReq(cifNumber, "0000"))))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(cifNumber, "0000"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/cif/reset-tpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq(cifNumber, "5678"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(cifRepository.findByCifNumber(cifNumber))
                .hasValueSatisfying(c -> {
                    assertThat(c.getStatus()).isEqualTo(CifStatus.ACTIVE);
                    assertThat(c.getFailedAttempts()).isZero();
                });

        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(cifNumber, "5678"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true));
    }

    @Test
    @DisplayName("Counter reset – correct TPIN after failures resets the counter")
    void flow_correctTpinAfterFailures_resetsCounter() throws Exception {
        String cifNumber = registerAndGetCifNumber("reset_counter@example.com");

        mockMvc.perform(post("/api/cif/set-tpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setTpinReq(cifNumber, "1234"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(cifNumber, "0000"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(cifNumber, "0000"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(cifNumber, "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true));

        assertThat(cifRepository.findByCifNumber(cifNumber))
                .hasValueSatisfying(c -> assertThat(c.getFailedAttempts()).isZero());

        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(cifNumber, "0000"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.remainingAttempts").value(2));
    }

    @Test
    @DisplayName("Register – duplicate email returns 409")
    void register_duplicateEmail_returns409() throws Exception {
        registerAndGetCifNumber("dup@example.com");

        mockMvc.perform(post("/api/cif/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq("dup@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Set TPIN – cannot set TPIN twice on same CIF")
    void setTpin_calledTwice_returns400() throws Exception {
        String cifNumber = registerAndGetCifNumber("twice@example.com");

        mockMvc.perform(post("/api/cif/set-tpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setTpinReq(cifNumber, "1234"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cif/set-tpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setTpinReq(cifNumber, "5678"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Authenticate – cannot authenticate before TPIN is set")
    void authenticate_beforeSetTpin_returns400() throws Exception {
        String cifNumber = registerAndGetCifNumber("notpin@example.com");

        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(cifNumber, "1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Reset TPIN – cannot reset TPIN on active (non-blocked) CIF")
    void resetTpin_activeCif_returns400() throws Exception {
        String cifNumber = registerAndGetCifNumber("active_reset@example.com");

        mockMvc.perform(post("/api/cif/set-tpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setTpinReq(cifNumber, "1234"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cif/reset-tpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq(cifNumber, "5678"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("All endpoints – unknown CIF number returns 404")
    void unknownCif_returns404_onAllEndpoints() throws Exception {
        final String unknown = "CIF-UNKNOWN1";

        mockMvc.perform(post("/api/cif/set-tpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setTpinReq(unknown, "1234"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/cif/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authReq(unknown, "1234"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/cif/reset-tpin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq(unknown, "1234"))))
                .andExpect(status().isNotFound());
    }
}
