#!/bin/bash

# Start SQL Server in the background
/opt/mssql/bin/sqlservr &
SQL_PID=$!

echo "Waiting for SQL Server to be ready..."
until /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -Q "SELECT 1" -C > /dev/null 2>&1; do
    sleep 2
done

echo "SQL Server is ready. Running init script..."
/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -i /init-db.sql -C
echo "Database initialized."

# Keep SQL Server running in the foreground
wait $SQL_PID
