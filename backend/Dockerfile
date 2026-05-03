# Build stage
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
COPY pom.xml .
# Download dependencies first (layer cache)
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
