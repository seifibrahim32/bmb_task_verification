package com.bmb.bank_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload for authentication")
public class AuthenticateResponse {

    @Schema(description = "Whether authentication was successful")
    private boolean authenticated;

    @Schema(description = "CIF number", example = "CIF-A1B2C3D4")
    private String cifNumber;

    @Schema(description = "Remaining allowed attempts before the account is blocked (null on success)")
    private Integer remainingAttempts;

    @Schema(description = "JWT bearer token — present only on successful authentication", example = "eyJhbGci...")
    private String token;

    public AuthenticateResponse(boolean authenticated, String cifNumber,
                                Integer remainingAttempts, String token) {
        this.authenticated    = authenticated;
        this.cifNumber        = cifNumber;
        this.remainingAttempts = remainingAttempts;
        this.token            = token;
    }

    public boolean isAuthenticated()       { return authenticated; }
    public String getCifNumber()           { return cifNumber; }
    public Integer getRemainingAttempts()  { return remainingAttempts; }
    public String getToken()               { return token; }
}
