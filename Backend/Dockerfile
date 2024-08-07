# Use OpenJDK 17 as the base image
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the JAR file into the container
COPY build/libs/*.jar /app/app.jar

# Copy the ...env file into the container
# COPY ./...env /app/...env

# Expose port 5000
EXPOSE 5000

# Set the entry point to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
