package com.bmb.bank_project.model;

public enum CifStatus {
    PENDING_TPIN,   // Registered but TPIN not yet set
    ACTIVE,         // Fully active
    BLOCKED         // Locked after 3 consecutive failed authentications
}
