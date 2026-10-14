# Multi-stage Docker build for Spring Boot application
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy the Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn/ .mvn/
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build the application
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/demo-0.0.1-SNAPSHOT.jar ./app.jar

# Create a non-root user to run the application
RUN addgroup -g 1001 -S spring && adduser -S spring -u 1001 -G spring

# Change ownership of the application files
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose the port the application runs on
EXPOSE 8080

# Set environment variables for JVM
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]