package com.bmb.bank_project.controller;

import com.bmb.bank_project.dto.*;
import com.bmb.bank_project.service.CifService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cif")
@Tag(name = "CIF Management", description = "Customer Information File registration, TPIN, and authentication")
public class CifController {

    private final CifService cifService;

    public CifController(CifService cifService) {
        this.cifService = cifService;
    }

    // 1. Register
    @PostMapping("/register")
    @Operation(
        summary = "Register a new CIF",
        description = "Creates a new Customer Information File. Returns a CIF number to use in subsequent calls."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "CIF created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        RegisterResponse data = cifService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("CIF registered successfully.", data));
    }

    // 2. Set TPIN
    @PostMapping("/set-tpin")
    @Operation(
        summary = "Set TPIN for a new CIF",
        description = "Sets the TPIN for a CIF that is in PENDING_TPIN status. Activates the CIF upon success."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "TPIN set and CIF activated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or TPIN already set"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CIF not found")
    })
    public ResponseEntity<ApiResponse<Void>> setTpin(
            @Valid @RequestBody SetTpinRequest request) {
        cifService.setTpin(request);
        return ResponseEntity.ok(ApiResponse.ok("TPIN set successfully. CIF is now active."));
    }

    // 3. Authenticate
    @PostMapping("/authenticate")
    @Operation(
        summary = "Authenticate CIF with TPIN",
        description = "Validates the CIF number and TPIN. Three consecutive failures will block the CIF."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication result returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "TPIN not set"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CIF blocked"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CIF not found")
    })
    public ResponseEntity<ApiResponse<AuthenticateResponse>> authenticate(
            @Valid @RequestBody AuthenticateRequest request) {
        AuthenticateResponse result = cifService.authenticate(request);
        if (result.isAuthenticated()) {
            return ResponseEntity.ok(ApiResponse.ok("Authentication successful.", result));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, "Incorrect TPIN. " + result.getRemainingAttempts()
                        + " attempt(s) remaining before account is blocked.", result));
    }

    // 4. Reset TPIN
    @PostMapping("/reset-tpin")
    @Operation(
        summary = "Reset TPIN and unblock CIF",
        description = "Unblocks a blocked CIF and sets a new TPIN. Only applicable to CIFs in BLOCKED status."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "TPIN reset and CIF unblocked"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CIF is not blocked or TPINs do not match"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CIF not found")
    })
    public ResponseEntity<ApiResponse<Void>> resetTpin(
            @Valid @RequestBody ResetTpinRequest request) {
        cifService.resetTpin(request);
        return ResponseEntity.ok(ApiResponse.ok("TPIN reset successfully. CIF is now active."));
    }
}
