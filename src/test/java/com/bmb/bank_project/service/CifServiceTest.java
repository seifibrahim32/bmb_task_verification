package com.bmb.bank_project.service;

import com.bmb.bank_project.dto.*;
import com.bmb.bank_project.exception.BankException;
import com.bmb.bank_project.model.Cif;
import com.bmb.bank_project.model.CifStatus;
import com.bmb.bank_project.repository.CifRepository;
import com.bmb.bank_project.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CifService Unit Tests")
class CifServiceTest {

    @Mock
    private CifRepository cifRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CifService cifService;

    private Cif activeCif(String cifNumber) {
        Cif cif = new Cif();
        cif.setCifNumber(cifNumber);
        cif.setFullName("John Doe");
        cif.setEmail("john@example.com");
        cif.setPhoneNumber("+9665XXXXXXXX");
        cif.setTpinHash("$2a$10$hashedTpin");
        cif.setStatus(CifStatus.ACTIVE);
        cif.setFailedAttempts(0);
        return cif;
    }

    private Cif pendingCif(String cifNumber) {
        Cif cif = activeCif(cifNumber);
        cif.setStatus(CifStatus.PENDING_TPIN);
        cif.setTpinHash(null);
        return cif;
    }

    private Cif blockedCif(String cifNumber) {
        Cif cif = activeCif(cifNumber);
        cif.setStatus(CifStatus.BLOCKED);
        cif.setFailedAttempts(3);
        return cif;
    }

    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Success – creates CIF with PENDING_TPIN status")
        void register_success() {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("John Doe");
            req.setEmail("john@example.com");
            req.setPhoneNumber("+9665XXXXXXXX");

            when(cifRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(cifRepository.findByCifNumber(anyString())).thenReturn(Optional.empty());
            when(cifRepository.save(any(Cif.class))).thenAnswer(i -> i.getArgument(0));

            RegisterResponse response = cifService.register(req);

            assertThat(response.getCifNumber()).startsWith("CIF-");
            assertThat(response.getFullName()).isEqualTo("John Doe");
            assertThat(response.getNextStep()).isNotBlank();

            verify(cifRepository).save(argThat(cif ->
                cif.getStatus() == CifStatus.PENDING_TPIN &&
                cif.getFailedAttempts() == 0 &&
                cif.getTpinHash() == null
            ));
        }

        @Test
        @DisplayName("Fail – duplicate email throws CONFLICT")
        void register_duplicateEmail_throwsConflict() {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("Jane Doe");
            req.setEmail("john@example.com");
            req.setPhoneNumber("+9665XXXXXXXX");

            when(cifRepository.existsByEmail("john@example.com")).thenReturn(true);

            BankException ex = catchThrowableOfType(
                    () -> cifService.register(req), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getMessage()).containsIgnoringCase("already exists");
            verify(cifRepository, never()).save(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("setTpin()")
    class SetTpinTests {

        @Test
        @DisplayName("Success – TPIN hashed, status becomes ACTIVE")
        void setTpin_success() {
            Cif cif = pendingCif("CIF-TEST01");
            when(cifRepository.findByCifNumber("CIF-TEST01")).thenReturn(Optional.of(cif));
            when(passwordEncoder.encode("1234")).thenReturn("$2a$10$hash");
            when(cifRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            SetTpinRequest req = new SetTpinRequest();
            req.setCifNumber("CIF-TEST01");
            req.setTpin("1234");
            req.setConfirmTpin("1234");

            assertThatNoException().isThrownBy(() -> cifService.setTpin(req));

            verify(cifRepository).save(argThat(c ->
                c.getStatus() == CifStatus.ACTIVE &&
                "$2a$10$hash".equals(c.getTpinHash()) &&
                c.getFailedAttempts() == 0
            ));
        }

        @Test
        @DisplayName("Fail – CIF not found throws NOT_FOUND")
        void setTpin_cifNotFound() {
            when(cifRepository.findByCifNumber("CIF-NONE")).thenReturn(Optional.empty());

            SetTpinRequest req = new SetTpinRequest();
            req.setCifNumber("CIF-NONE");
            req.setTpin("1234");
            req.setConfirmTpin("1234");

            BankException ex = catchThrowableOfType(
                () -> cifService.setTpin(req), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Fail – CIF already ACTIVE throws BAD_REQUEST")
        void setTpin_alreadyActive_throwsBadRequest() {
            when(cifRepository.findByCifNumber("CIF-TEST01"))
                .thenReturn(Optional.of(activeCif("CIF-TEST01")));

            SetTpinRequest req = new SetTpinRequest();
            req.setCifNumber("CIF-TEST01");
            req.setTpin("1234");
            req.setConfirmTpin("1234");

            BankException ex = catchThrowableOfType(
                () -> cifService.setTpin(req), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).containsIgnoringCase("already been set");
        }

        @Test
        @DisplayName("Fail – TPIN and confirmTpin mismatch throws BAD_REQUEST")
        void setTpin_mismatch_throwsBadRequest() {
            when(cifRepository.findByCifNumber("CIF-TEST01"))
                .thenReturn(Optional.of(pendingCif("CIF-TEST01")));

            SetTpinRequest req = new SetTpinRequest();
            req.setCifNumber("CIF-TEST01");
            req.setTpin("1234");
            req.setConfirmTpin("9999");

            BankException ex = catchThrowableOfType(
                () -> cifService.setTpin(req), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).containsIgnoringCase("do not match");
            verify(passwordEncoder, never()).encode(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("authenticate()")
    class AuthenticateTests {

        private AuthenticateRequest req(String cifNumber, String tpin) {
            AuthenticateRequest r = new AuthenticateRequest();
            r.setCifNumber(cifNumber);
            r.setTpin(tpin);
            return r;
        }

        @Test
        @DisplayName("Success – correct TPIN resets failedAttempts")
        void authenticate_correctTpin_succeeds() {
            Cif cif = activeCif("CIF-TEST01");
            cif.setFailedAttempts(1);
            when(cifRepository.findByCifNumber("CIF-TEST01")).thenReturn(Optional.of(cif));
            when(passwordEncoder.matches("1234", "$2a$10$hashedTpin")).thenReturn(true);
            when(cifRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            AuthenticateResponse result = cifService.authenticate(req("CIF-TEST01", "1234"));

            assertThat(result.isAuthenticated()).isTrue();
            assertThat(result.getRemainingAttempts()).isNull();
            verify(cifRepository).save(argThat(c -> c.getFailedAttempts() == 0));
        }

        @Test
        @DisplayName("Fail – wrong TPIN (1st attempt), 2 attempts remaining")
        void authenticate_wrongTpin_firstAttempt() {
            Cif cif = activeCif("CIF-TEST01");
            when(cifRepository.findByCifNumber("CIF-TEST01")).thenReturn(Optional.of(cif));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            when(cifRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            AuthenticateResponse result = cifService.authenticate(req("CIF-TEST01", "0000"));

            assertThat(result.isAuthenticated()).isFalse();
            assertThat(result.getRemainingAttempts()).isEqualTo(2);
            verify(cifRepository).save(argThat(c ->
                c.getFailedAttempts() == 1 && c.getStatus() == CifStatus.ACTIVE));
        }

        @Test
        @DisplayName("Fail – wrong TPIN (2nd attempt), 1 attempt remaining")
        void authenticate_wrongTpin_secondAttempt() {
            Cif cif = activeCif("CIF-TEST01");
            cif.setFailedAttempts(1);
            when(cifRepository.findByCifNumber("CIF-TEST01")).thenReturn(Optional.of(cif));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            when(cifRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            AuthenticateResponse result = cifService.authenticate(req("CIF-TEST01", "0000"));

            assertThat(result.isAuthenticated()).isFalse();
            assertThat(result.getRemainingAttempts()).isEqualTo(1);
            verify(cifRepository).save(argThat(c ->
                c.getFailedAttempts() == 2 && c.getStatus() == CifStatus.ACTIVE));
        }

        @Test
        @DisplayName("Fail – wrong TPIN (3rd attempt) blocks CIF and throws FORBIDDEN")
        void authenticate_wrongTpin_thirdAttempt_blocksCif() {
            Cif cif = activeCif("CIF-TEST01");
            cif.setFailedAttempts(2);
            when(cifRepository.findByCifNumber("CIF-TEST01")).thenReturn(Optional.of(cif));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
            when(cifRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            BankException ex = catchThrowableOfType(
                () -> cifService.authenticate(req("CIF-TEST01", "0000")), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(ex.getMessage()).containsIgnoringCase("blocked");
            verify(cifRepository).save(argThat(c -> c.getStatus() == CifStatus.BLOCKED));
        }

        @Test
        @DisplayName("Fail – BLOCKED CIF cannot authenticate, throws FORBIDDEN")
        void authenticate_blockedCif_throwsForbidden() {
            when(cifRepository.findByCifNumber("CIF-TEST01"))
                .thenReturn(Optional.of(blockedCif("CIF-TEST01")));

            BankException ex = catchThrowableOfType(
                () -> cifService.authenticate(req("CIF-TEST01", "1234")), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("Fail – PENDING_TPIN CIF throws BAD_REQUEST")
        void authenticate_pendingTpin_throwsBadRequest() {
            when(cifRepository.findByCifNumber("CIF-TEST01"))
                .thenReturn(Optional.of(pendingCif("CIF-TEST01")));

            BankException ex = catchThrowableOfType(
                () -> cifService.authenticate(req("CIF-TEST01", "1234")), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Fail – CIF not found throws NOT_FOUND")
        void authenticate_cifNotFound_throwsNotFound() {
            when(cifRepository.findByCifNumber("CIF-NONE")).thenReturn(Optional.empty());

            BankException ex = catchThrowableOfType(
                () -> cifService.authenticate(req("CIF-NONE", "1234")), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Success after prior failures – failedAttempts resets to 0")
        void authenticate_successAfterPriorFailures_resetsCounter() {
            Cif cif = activeCif("CIF-TEST01");
            cif.setFailedAttempts(2);
            when(cifRepository.findByCifNumber("CIF-TEST01")).thenReturn(Optional.of(cif));
            when(passwordEncoder.matches("1234", "$2a$10$hashedTpin")).thenReturn(true);
            when(cifRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            AuthenticateResponse result = cifService.authenticate(req("CIF-TEST01", "1234"));

            assertThat(result.isAuthenticated()).isTrue();
            verify(cifRepository).save(argThat(c ->
                c.getFailedAttempts() == 0 && c.getStatus() == CifStatus.ACTIVE));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("resetTpin()")
    class ResetTpinTests {

        private ResetTpinRequest req(String cifNumber, String newTpin, String confirm) {
            ResetTpinRequest r = new ResetTpinRequest();
            r.setCifNumber(cifNumber);
            r.setNewTpin(newTpin);
            r.setConfirmTpin(confirm);
            return r;
        }

        @Test
        @DisplayName("Success – unblocks CIF, sets new TPIN, failedAttempts = 0")
        void resetTpin_success() {
            when(cifRepository.findByCifNumber("CIF-TEST01"))
                .thenReturn(Optional.of(blockedCif("CIF-TEST01")));
            when(passwordEncoder.encode("5678")).thenReturn("$2a$10$newHash");
            when(cifRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            assertThatNoException().isThrownBy(() -> cifService.resetTpin(req("CIF-TEST01", "5678", "5678")));

            verify(cifRepository).save(argThat(c ->
                c.getStatus() == CifStatus.ACTIVE &&
                c.getFailedAttempts() == 0 &&
                "$2a$10$newHash".equals(c.getTpinHash())
            ));
        }

        @Test
        @DisplayName("Fail – CIF not found throws NOT_FOUND")
        void resetTpin_cifNotFound() {
            when(cifRepository.findByCifNumber("CIF-NONE")).thenReturn(Optional.empty());

            BankException ex = catchThrowableOfType(
                () -> cifService.resetTpin(req("CIF-NONE", "5678", "5678")), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Fail – ACTIVE CIF is not blocked, throws BAD_REQUEST")
        void resetTpin_cifNotBlocked_throwsBadRequest() {
            when(cifRepository.findByCifNumber("CIF-TEST01"))
                .thenReturn(Optional.of(activeCif("CIF-TEST01")));

            BankException ex = catchThrowableOfType(
                () -> cifService.resetTpin(req("CIF-TEST01", "5678", "5678")), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).containsIgnoringCase("not blocked");
        }

        @Test
        @DisplayName("Fail – PENDING_TPIN CIF throws BAD_REQUEST (use Set TPIN instead)")
        void resetTpin_pendingTpin_throwsBadRequest() {
            when(cifRepository.findByCifNumber("CIF-TEST01"))
                .thenReturn(Optional.of(pendingCif("CIF-TEST01")));

            BankException ex = catchThrowableOfType(
                () -> cifService.resetTpin(req("CIF-TEST01", "5678", "5678")), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Fail – new TPIN and confirmTpin mismatch throws BAD_REQUEST")
        void resetTpin_mismatch_throwsBadRequest() {
            when(cifRepository.findByCifNumber("CIF-TEST01"))
                .thenReturn(Optional.of(blockedCif("CIF-TEST01")));

            BankException ex = catchThrowableOfType(
                () -> cifService.resetTpin(req("CIF-TEST01", "5678", "0000")), BankException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).containsIgnoringCase("do not match");
            verify(passwordEncoder, never()).encode(any());
        }
    }
}
