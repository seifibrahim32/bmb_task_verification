package com.bmb.bank_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "Request payload for opening a new bank account")
public class CreateAccountRequest {

    @NotBlank(message = "Account number is required")
    @Size(min = 3, max = 20, message = "Account number must be 3–20 characters")
    @Pattern(regexp = "[A-Za-z0-9\\-]+",
             message = "Account number may only contain letters, digits, and hyphens")
    @Schema(description = "Globally unique account number", example = "ACC-001234")
    private String accountNumber;

    @NotBlank(message = "Holder name is required")
    @Size(max = 100, message = "Holder name must not exceed 100 characters")
    @Schema(description = "Full name of the account holder", example = "John Doe")
    private String holderName;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00", message = "Balance must be zero or positive")
    @Schema(description = "Opening balance (≥ 0)", example = "1500.00")
    private BigDecimal balance;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "SAVINGS|CURRENT",
             message = "accountType must be SAVINGS or CURRENT")
    @Schema(description = "Account type", example = "SAVINGS",
            allowableValues = {"SAVINGS", "CURRENT"})
    private String accountType;

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
}
