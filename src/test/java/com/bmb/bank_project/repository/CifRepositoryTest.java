package com.bmb.bank_project.repository;

import com.bmb.bank_project.model.Cif;
import com.bmb.bank_project.model.CifStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("CifRepository Data Layer Tests")
class CifRepositoryTest {

    @Autowired
    private CifRepository cifRepository;

    private Cif savedCif;

    @BeforeEach
    void setUp() {
        cifRepository.deleteAll();

        Cif cif = new Cif();
        cif.setCifNumber("CIF-TEST01");
        cif.setFullName("John Doe");
        cif.setEmail("john@example.com");
        cif.setPhoneNumber("+9665XXXXXXXX");
        cif.setStatus(CifStatus.ACTIVE);
        cif.setTpinHash("$2a$10$someHashedTpin");
        cif.setFailedAttempts(0);
        savedCif = cifRepository.save(cif);
    }

    @Test
    @DisplayName("findByCifNumber – returns CIF when it exists")
    void findByCifNumber_exists_returnsCif() {
        Optional<Cif> result = cifRepository.findByCifNumber("CIF-TEST01");

        assertThat(result).isPresent();
        assertThat(result.get().getCifNumber()).isEqualTo("CIF-TEST01");
        assertThat(result.get().getFullName()).isEqualTo("John Doe");
        assertThat(result.get().getStatus()).isEqualTo(CifStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByCifNumber – returns empty when CIF does not exist")
    void findByCifNumber_notExists_returnsEmpty() {
        Optional<Cif> result = cifRepository.findByCifNumber("CIF-UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail – returns true when email exists")
    void existsByEmail_exists_returnsTrue() {
        assertThat(cifRepository.existsByEmail("john@example.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail – returns false when email does not exist")
    void existsByEmail_notExists_returnsFalse() {
        assertThat(cifRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    @DisplayName("existsByEmail – case-sensitive match")
    void existsByEmail_differentCase_returnsFalse() {
        assertThat(cifRepository.existsByEmail("JOHN@EXAMPLE.COM")).isFalse();
    }

    @Test
    @DisplayName("save – persists all fields correctly")
    void save_persistsAllFields() {
        assertThat(savedCif.getId()).isNotNull();
        assertThat(savedCif.getCreatedAt()).isNotNull();
        assertThat(savedCif.getUpdatedAt()).isNotNull();
        assertThat(savedCif.getFailedAttempts()).isZero();
    }

    @Test
    @DisplayName("save – status update persists correctly")
    void save_statusUpdate_persists() {
        savedCif.setStatus(CifStatus.BLOCKED);
        savedCif.setFailedAttempts(3);
        cifRepository.save(savedCif);

        Cif reloaded = cifRepository.findByCifNumber("CIF-TEST01").orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CifStatus.BLOCKED);
        assertThat(reloaded.getFailedAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("unique constraint – second CIF with same email cannot be saved")
    void save_duplicateEmail_throwsException() {
        Cif duplicate = new Cif();
        duplicate.setCifNumber("CIF-TEST02");
        duplicate.setFullName("Jane Doe");
        duplicate.setEmail("john@example.com");
        duplicate.setPhoneNumber("+9665YYYYYYYY");
        duplicate.setStatus(CifStatus.PENDING_TPIN);

        assertThatThrownBy(() -> cifRepository.saveAndFlush(duplicate))
                .isInstanceOf(Exception.class);
    }
}
