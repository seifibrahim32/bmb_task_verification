package com.bmb.bank_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload after successful CIF registration")
public class RegisterResponse {

    @Schema(description = "Generated CIF number", example = "CIF-A1B2C3D4")
    private String cifNumber;

    @Schema(description = "Customer full name", example = "John Doe")
    private String fullName;

    @Schema(description = "Next step instruction")
    private String nextStep;

    public RegisterResponse(String cifNumber, String fullName) {
        this.cifNumber = cifNumber;
        this.fullName = fullName;
        this.nextStep = "Use the Set TPIN API with your CIF number to activate your account.";
    }

    public String getCifNumber() { return cifNumber; }
    public String getFullName() { return fullName; }
    public String getNextStep() { return nextStep; }
}
