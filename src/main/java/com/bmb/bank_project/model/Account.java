package com.bmb.bank_project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
@Schema(description = "Bank account entity")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique account ID", example = "1")
    private Long id;

    @Column(nullable = false, unique = true)
    @Schema(description = "Account number", example = "ACC-001234")
    private String accountNumber;

    @Column(nullable = false)
    @Schema(description = "Account holder full name", example = "John Doe")
    private String holderName;

    @Column(nullable = false)
    @Schema(description = "Current balance", example = "1500.00")
    private BigDecimal balance;

    @Column(nullable = false)
    @Schema(description = "Account type: SAVINGS or CURRENT", example = "SAVINGS")
    private String accountType;

    /** FK → cifs.id.  Hidden from JSON serialisation to avoid cycles. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cif_id", nullable = false)
    @JsonIgnore
    private Cif cif;

    public Account() {}

    public Account(String accountNumber, String holderName,
                   BigDecimal balance, String accountType) {
        this.accountNumber = accountNumber;
        this.holderName    = holderName;
        this.balance       = balance;
        this.accountType   = accountType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public Cif getCif() { return cif; }
    public void setCif(Cif cif) { this.cif = cif; }
}
