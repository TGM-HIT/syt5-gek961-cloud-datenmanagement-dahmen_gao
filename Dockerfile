# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the jar file to the container
COPY /build/libs/dezsysauth.jar /app/dezsysauth.jar

# Expose the port on which the app runs
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "dezsysauth.jar"]
