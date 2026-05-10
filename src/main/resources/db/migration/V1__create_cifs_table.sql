-- ─────────────────────────────────────────────────────────────────────────────
-- V1 : Create the cifs table
--      Stores Customer Information Files (CIF) with TPIN state machine:
--      PENDING_TPIN → ACTIVE ↔ BLOCKED
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE cifs (
    id              BIGINT          IDENTITY(1,1)   NOT NULL,
    cif_number      NVARCHAR(20)                    NOT NULL,
    full_name       NVARCHAR(100)                   NOT NULL,
    email           NVARCHAR(100)                   NOT NULL,
    phone_number    NVARCHAR(20)                    NOT NULL,
    tpin_hash       NVARCHAR(255)                   NULL,
    status          NVARCHAR(20)                    NOT NULL,
    failed_attempts INT                             NOT NULL    DEFAULT 0,
    created_at      DATETIME2                       NOT NULL,
    updated_at      DATETIME2                       NULL,

    CONSTRAINT PK_cifs               PRIMARY KEY (id),
    CONSTRAINT UQ_cifs_cif_number    UNIQUE      (cif_number),
    CONSTRAINT UQ_cifs_email         UNIQUE      (email)
);
