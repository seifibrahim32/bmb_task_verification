-- Create the database if it doesn't exist
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'bank_db')
BEGIN
    CREATE DATABASE bank_db;
    PRINT 'Database bank_db created.';
END
ELSE
BEGIN
    PRINT 'Database bank_db already exists.';
END
GO

USE bank_db;
GO

-- Hibernate will auto-create tables via ddl-auto=update
-- You can add seed data here if needed

-- Example seed (optional, uncomment to use):
-- INSERT INTO accounts (account_number, holder_name, balance, account_type)
-- VALUES ('ACC-000001', 'Alice Smith', 5000.00, 'SAVINGS'),
--        ('ACC-000002', 'Bob Johnson', 12000.00, 'CURRENT');
-- GO
