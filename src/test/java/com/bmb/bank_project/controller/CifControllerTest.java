package com.bmb.bank_project.controller;

import com.bmb.bank_project.config.SecurityConfig;
import com.bmb.bank_project.dto.AuthenticateRequest;
import com.bmb.bank_project.dto.AuthenticateResponse;
import com.bmb.bank_project.dto.RegisterRequest;
import com.bmb.bank_project.dto.RegisterResponse;
import com.bmb.bank_project.dto.ResetTpinRequest;
import com.bmb.bank_project.dto.SetTpinRequest;
import com.bmb.bank_project.exception.BankException;
import com.bmb.bank_project.security.JwtUtil;
import com.bmb.bank_project.service.CifService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CifController.class)
@Import(SecurityConfig.class)
@DisplayName("CifController Web Layer Tests")
class CifControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CifService cifService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/cif/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("201 – valid request returns cifNumber and nextStep")
        void register_validRequest_returns201() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("John Doe");
            req.setEmail("john@example.com");
            req.setPhoneNumber("+9665XXXXXXXX");

            when(cifService.register(any())).thenReturn(new RegisterResponse("CIF-A1B2C3D4", "John Doe"));

            mockMvc.perform(post("/api/cif/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.cifNumber").value("CIF-A1B2C3D4"))
                    .andExpect(jsonPath("$.data.nextStep").isNotEmpty());
        }

        @Test
        @DisplayName("400 – missing fullName fails validation")
        void register_missingFullName_returns400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("john@example.com");
            req.setPhoneNumber("+9665XXXXXXXX");

            mockMvc.perform(post("/api/cif/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("400 – invalid email format fails validation")
        void register_invalidEmail_returns400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("John Doe");
            req.setEmail("not-an-email");
            req.setPhoneNumber("+9665XXXXXXXX");

            mockMvc.perform(post("/api/cif/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – phone number with letters fails validation")
        void register_invalidPhone_returns400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("John Doe");
            req.setEmail("john@example.com");
            req.setPhoneNumber("ABC-WRONG");

            mockMvc.perform(post("/api/cif/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("409 – duplicate email propagates CONFLICT from service")
        void register_duplicateEmail_returns409() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("John Doe");
            req.setEmail("john@example.com");
            req.setPhoneNumber("+9665XXXXXXXX");

            when(cifService.register(any()))
                    .thenThrow(new BankException("A CIF with this email already exists.", HttpStatus.CONFLICT));

            mockMvc.perform(post("/api/cif/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message", containsStringIgnoringCase("already exists")));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/cif/set-tpin")
    class SetTpinEndpointTests {

        @Test
        @DisplayName("200 – valid request activates CIF")
        void setTpin_validRequest_returns200() throws Exception {
            SetTpinRequest req = new SetTpinRequest();
            req.setCifNumber("CIF-A1B2C3D4");
            req.setTpin("1234");
            req.setConfirmTpin("1234");

            doNothing().when(cifService).setTpin(any());

            mockMvc.perform(post("/api/cif/set-tpin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message", containsStringIgnoringCase("active")));
        }

        @Test
        @DisplayName("400 – TPIN shorter than 4 digits fails validation")
        void setTpin_tpinTooShort_returns400() throws Exception {
            SetTpinRequest req = new SetTpinRequest();
            req.setCifNumber("CIF-A1B2C3D4");
            req.setTpin("12");
            req.setConfirmTpin("12");

            mockMvc.perform(post("/api/cif/set-tpin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – non-numeric TPIN fails validation")
        void setTpin_nonNumericTpin_returns400() throws Exception {
            SetTpinRequest req = new SetTpinRequest();
            req.setCifNumber("CIF-A1B2C3D4");
            req.setTpin("abcd");
            req.setConfirmTpin("abcd");

            mockMvc.perform(post("/api/cif/set-tpin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 – missing cifNumber fails validation")
        void setTpin_missingCifNumber_returns400() throws Exception {
            SetTpinRequest req = new SetTpinRequest();
            req.setTpin("1234");
            req.setConfirmTpin("1234");

            mockMvc.perform(post("/api/cif/set-tpin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 – CIF not found propagates from service")
        void setTpin_cifNotFound_returns404() throws Exception {
            SetTpinRequest req = new SetTpinRequest();
            req.setCifNumber("CIF-NOTFOUND");
            req.setTpin("1234");
            req.setConfirmTpin("1234");

            doThrow(new BankException("CIF not found", HttpStatus.NOT_FOUND))
                    .when(cifService).setTpin(any());

            mockMvc.perform(post("/api/cif/set-tpin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/cif/authenticate")
    class AuthenticateEndpointTests {

        @Test
        @DisplayName("200 – correct TPIN returns authenticated=true")
        void authenticate_success_returns200() throws Exception {
            AuthenticateRequest req = new AuthenticateRequest();
            req.setCifNumber("CIF-A1B2C3D4");
            req.setTpin("1234");

            when(cifService.authenticate(any()))
                    .thenReturn(new AuthenticateResponse(true, "CIF-A1B2C3D4", null, "mock-jwt-token"));

            mockMvc.perform(post("/api/cif/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.authenticated").value(true));
        }

        @Test
        @DisplayName("401 – wrong TPIN returns authenticated=false with remaining attempts")
        void authenticate_wrongTpin_returns401() throws Exception {
            AuthenticateRequest req = new AuthenticateRequest();
            req.setCifNumber("CIF-A1B2C3D4");
            req.setTpin("0000");

            when(cifService.authenticate(any()))
                    .thenReturn(new AuthenticateResponse(false, "CIF-A1B2C3D4", 2, null));

            mockMvc.perform(post("/api/cif/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.data.authenticated").value(false))
                    .andExpect(jsonPath("$.data.remainingAttempts").value(2));
        }

        @Test
        @DisplayName("403 – blocked CIF throws FORBIDDEN")
        void authenticate_blockedCif_returns403() throws Exception {
            AuthenticateRequest req = new AuthenticateRequest();
            req.setCifNumber("CIF-A1B2C3D4");
            req.setTpin("1234");

            when(cifService.authenticate(any()))
                    .thenThrow(new BankException("CIF is blocked.", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/api/cif/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 – TPIN longer than 6 digits fails validation")
        void authenticate_tpinTooLong_returns400() throws Exception {
            AuthenticateRequest req = new AuthenticateRequest();
            req.setCifNumber("CIF-A1B2C3D4");
            req.setTpin("1234567");

            mockMvc.perform(post("/api/cif/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/cif/reset-tpin")
    class ResetTpinEndpointTests {

        @Test
        @DisplayName("200 – blocked CIF is unblocked with new TPIN")
        void resetTpin_success_returns200() throws Exception {
            ResetTpinRequest req = new ResetTpinRequest();
            req.setCifNumber("CIF-A1B2C3D4");
            req.setNewTpin("5678");
            req.setConfirmTpin("5678");

            doNothing().when(cifService).resetTpin(any());

            mockMvc.perform(post("/api/cif/reset-tpin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("400 – CIF not blocked propagates BAD_REQUEST from service")
        void resetTpin_notBlocked_returns400() throws Exception {
            ResetTpinRequest req = new ResetTpinRequest();
            req.setCifNumber("CIF-A1B2C3D4");
            req.setNewTpin("5678");
            req.setConfirmTpin("5678");

            doThrow(new BankException("CIF is not blocked.", HttpStatus.BAD_REQUEST))
                    .when(cifService).resetTpin(any());

            mockMvc.perform(post("/api/cif/reset-tpin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("400 – non-numeric new TPIN fails validation")
        void resetTpin_nonNumericTpin_returns400() throws Exception {
            ResetTpinRequest req = new ResetTpinRequest();
            req.setCifNumber("CIF-A1B2C3D4");
            req.setNewTpin("abcd");
            req.setConfirmTpin("abcd");

            mockMvc.perform(post("/api/cif/reset-tpin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 – CIF not found propagates from service")
        void resetTpin_cifNotFound_returns404() throws Exception {
            ResetTpinRequest req = new ResetTpinRequest();
            req.setCifNumber("CIF-NOTFOUND");
            req.setNewTpin("5678");
            req.setConfirmTpin("5678");

            doThrow(new BankException("CIF not found", HttpStatus.NOT_FOUND))
                    .when(cifService).resetTpin(any());

            mockMvc.perform(post("/api/cif/reset-tpin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }
}
