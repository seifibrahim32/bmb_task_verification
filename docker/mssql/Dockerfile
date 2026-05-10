FROM mcr.microsoft.com/mssql/server:2022-latest

ENV ACCEPT_EULA=Y
ENV MSSQL_PID=Developer

USER root

COPY init-db.sql /init-db.sql
COPY entrypoint.sh /entrypoint.sh
RUN sed -i 's/\r//' /entrypoint.sh && chmod +x /entrypoint.sh

USER mssql

EXPOSE 1433

ENTRYPOINT ["/entrypoint.sh"]
