# build stage
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
# download dependencies first (layer cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# runtime stage, no need for maven
FROM eclipse-temurin:25-jre
RUN groupadd -r omiyage && useradd -r -g omiyage omiyage
WORKDIR /app
# copy jar and set ownership
COPY --from=build --chown=omiyage:omiyage /app/target/*.jar app.jar
# switch to non-root user
USER omiyage

ENV SERVER_PORT=8080
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
