package com.bmb.bank_project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cifs")
@Schema(description = "Customer Information File (CIF)")
public class Cif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cif_number", unique = true, nullable = false, length = 20)
    @Schema(description = "Unique CIF identifier", example = "CIF-A1B2C3D4")
    private String cifNumber;

    @Column(name = "full_name", nullable = false, length = 100)
    @Schema(description = "Customer full name", example = "John Doe")
    private String fullName;

    @Column(unique = true, nullable = false, length = 100)
    @Schema(description = "Customer email address", example = "john.doe@example.com")
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    @Schema(description = "Customer phone number", example = "+9665XXXXXXXX")
    private String phoneNumber;

    @Column(name = "tpin_hash")
    private String tpinHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Schema(description = "CIF status: PENDING_TPIN, ACTIVE, or BLOCKED")
    private CifStatus status;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** One CIF owns zero-or-more bank accounts (FK: accounts.cif_id → cifs.id). */
    @OneToMany(mappedBy = "cif", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Account> accounts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Cif() {}

    public Long getId() { return id; }

    public String getCifNumber() { return cifNumber; }
    public void setCifNumber(String cifNumber) { this.cifNumber = cifNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getTpinHash() { return tpinHash; }
    public void setTpinHash(String tpinHash) { this.tpinHash = tpinHash; }

    public CifStatus getStatus() { return status; }
    public void setStatus(CifStatus status) { this.status = status; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public List<Account> getAccounts() { return accounts; }
    public void setAccounts(List<Account> accounts) { this.accounts = accounts; }
}
