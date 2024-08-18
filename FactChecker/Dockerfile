# Build stage
FROM gradle:8.5-jdk21 AS build

# Install Node.js and npm
RUN apt-get update && apt-get install -y nodejs npm

# Set the working directory
WORKDIR /app

# Copy package.json and package-lock.json (if you have one)
COPY package.json package-lock.json* ./

# Install npm dependencies
RUN npm install

# Run the CSS build
RUN npm run build-css

# Copy the build files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy the Checkstyle configuration
COPY config ./config

# Now copy the rest of the source code
COPY src ./src

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
