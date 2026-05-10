package com.bmb.bank_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Bank account details returned by the API")
public class AccountResponse {

    @Schema(description = "Auto-generated account ID", example = "1")
    private Long id;

    @Schema(description = "Account number", example = "ACC-001234")
    private String accountNumber;

    @Schema(description = "Account holder full name", example = "John Doe")
    private String holderName;

    @Schema(description = "Current balance", example = "1500.00")
    private BigDecimal balance;

    @Schema(description = "Account type: SAVINGS or CURRENT", example = "SAVINGS")
    private String accountType;

    @Schema(description = "CIF number of the owning customer", example = "CIF-A1B2C3D4")
    private String cifNumber;

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

    public String getCifNumber() { return cifNumber; }
    public void setCifNumber(String cifNumber) { this.cifNumber = cifNumber; }
}
