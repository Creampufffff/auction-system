FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY auction-common/pom.xml auction-common/pom.xml
COPY auction-server/pom.xml auction-server/pom.xml
RUN mvn -pl auction-server -am dependency:go-offline

COPY auction-common auction-common
COPY auction-server auction-server
RUN mvn -pl auction-server -am package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /app/auction-server/target/auction-server-1.0-SNAPSHOT.jar auction-server.jar
COPY --from=build /app/auction-server/target/dependency dependency

EXPOSE 5000

CMD ["java", "-cp", "auction-server.jar:dependency/*", "com.auction.app.AuctionApplication"]
