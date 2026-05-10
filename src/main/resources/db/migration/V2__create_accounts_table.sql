-- ─────────────────────────────────────────────────────────────────────────────
-- V2 : Create the accounts table
--      Each account belongs to exactly one CIF (Many-to-One).
--      account_number is globally unique across all CIFs.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE accounts (
    id              BIGINT          IDENTITY(1,1)   NOT NULL,
    account_number  NVARCHAR(255)                   NOT NULL,
    holder_name     NVARCHAR(255)                   NOT NULL,
    balance         DECIMAL(19, 2)                  NOT NULL,
    account_type    NVARCHAR(255)                   NOT NULL,
    cif_id          BIGINT                          NOT NULL,

    CONSTRAINT PK_accounts                  PRIMARY KEY (id),
    CONSTRAINT UQ_accounts_account_number   UNIQUE      (account_number),
    CONSTRAINT FK_accounts_cif              FOREIGN KEY (cif_id)
                                            REFERENCES  dbo.cifs(id)
);
