package com.bmb.bank_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request payload for resetting TPIN and unblocking a CIF")
public class ResetTpinRequest {

    @NotBlank(message = "CIF number is required")
    @Schema(description = "CIF number to unblock", example = "CIF-A1B2C3D4")
    private String cifNumber;

    @NotBlank(message = "New TPIN is required")
    @Pattern(regexp = "\\d{4,6}", message = "TPIN must be 4 to 6 digits")
    @Schema(description = "New 4–6 digit numeric TPIN", example = "5678")
    private String newTpin;

    @NotBlank(message = "Confirm new TPIN is required")
    @Schema(description = "Must match newTpin exactly", example = "5678")
    private String confirmTpin;

    public String getCifNumber() { return cifNumber; }
    public void setCifNumber(String cifNumber) { this.cifNumber = cifNumber; }

    public String getNewTpin() { return newTpin; }
    public void setNewTpin(String newTpin) { this.newTpin = newTpin; }

    public String getConfirmTpin() { return confirmTpin; }
    public void setConfirmTpin(String confirmTpin) { this.confirmTpin = confirmTpin; }
}
