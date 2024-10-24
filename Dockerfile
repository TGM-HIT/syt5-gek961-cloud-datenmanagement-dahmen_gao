# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Install necessary utilities
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    && rm -rf /var/lib/apt/lists/*

# Set the working directory inside the container
WORKDIR /app

# Copy the entire application code into the container
COPY . /app

# Ensure the Gradle wrapper is executable
RUN chmod +x gradlew

# Build the application using the Gradle wrapper
RUN ./gradlew build --no-daemon

# Expose the port on which the app runs
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "build/libs/dezsysauth.jar"]