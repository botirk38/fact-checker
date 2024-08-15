# Build stage
FROM gradle:8.5-jdk21 AS build

# Set the working directory
WORKDIR /app

# Copy the build files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy the source code
COPY src ./src

# Set OpenAI Key

ENV OPENAI_KEY=${OPENAI_API_KEY:OPENAI_API_KEY}

# Build the application
RUN gradle build --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the built artifact from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"


# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]




