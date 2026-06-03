FROM mysql:8.0

ENV TZ=Asia/Ho_Chi_Minh

COPY auction-server/schema.sql /docker-entrypoint-initdb.d/01-schema.sql
