# Build stage
FROM gradle:8.5-jdk21 AS build

# Install Node.js in a single layer
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get update && \
    apt-get install -y nodejs && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy only necessary files for npm install
COPY package.json package-lock.json* tailwind.config.js ./
RUN npm ci --only=production

# Copy and build CSS
COPY src/main/resources/static/css ./src/main/resources/static/css
COPY src/main/resources/templates ./src/main/resources/templates
RUN npm run build-css

# Copy Gradle files and download dependencies
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon

# Copy source code and build
COPY config ./config
COPY src ./src
RUN gradle build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built artifact from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
