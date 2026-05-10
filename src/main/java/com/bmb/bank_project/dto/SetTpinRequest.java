package com.bmb.bank_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request payload for setting a TPIN on a new CIF")
public class SetTpinRequest {

    @NotBlank(message = "CIF number is required")
    @Schema(description = "CIF number obtained during registration", example = "CIF-A1B2C3D4")
    private String cifNumber;

    @NotBlank(message = "TPIN is required")
    @Pattern(regexp = "\\d{4,6}", message = "TPIN must be 4 to 6 digits")
    @Schema(description = "4–6 digit numeric TPIN", example = "1234")
    private String tpin;

    @NotBlank(message = "Confirm TPIN is required")
    @Schema(description = "Must match tpin exactly", example = "1234")
    private String confirmTpin;

    public String getCifNumber() { return cifNumber; }
    public void setCifNumber(String cifNumber) { this.cifNumber = cifNumber; }

    public String getTpin() { return tpin; }
    public void setTpin(String tpin) { this.tpin = tpin; }

    public String getConfirmTpin() { return confirmTpin; }
    public void setConfirmTpin(String confirmTpin) { this.confirmTpin = confirmTpin; }
}
