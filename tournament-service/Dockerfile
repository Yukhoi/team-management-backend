FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY gateway-service ./gateway-service
COPY identity-service ./identity-service
COPY team-service ./team-service
COPY tournament-service ./tournament-service
COPY match-service ./match-service
COPY statistics-service ./statistics-service
COPY audit-service ./audit-service
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -pl tournament-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
COPY --from=build /app/tournament-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
