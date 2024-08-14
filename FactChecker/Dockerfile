FROM eclipse-temurin:21-jre-jammy AS builder
WORKDIR /app

# List contents of the current directory
RUN ls -la

# Try to list contents of the build directory, if it exists
RUN ls -la build/libs || echo "build/libs directory does not exist"

# Copy the JAR file
COPY FactChecker-0.0.1-SNAPSHOT.jar app.jar

# List contents again to confirm the JAR was copied
RUN ls -la

RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy layers from the builder stage
COPY --from=builder app/dependencies/ ./
COPY --from=builder app/spring-boot-loader/ ./
COPY --from=builder app/snapshot-dependencies/ ./
COPY --from=builder app/application/ ./

# List contents of the final image
RUN ls -la

# Railway specific environment variable
ENV PORT=8080

# Expose the port
EXPOSE ${PORT}

# Start the application
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
