# Build stage
FROM gradle:8.5-jdk21 AS build
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs

# Set the working directory
WORKDIR /app

# Copy package.json, package-lock.json, and tailwind.config.js
COPY package.json package-lock.json* tailwind.config.js ./

# Install npm dependencies
RUN npm install

# Copy the CSS source files
COPY src/main/resources/static/css ./src/main/resources/static/css
COPY src/main/resources/templates ./src/main/resources/templates

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
