FROM eclipse-temurin:21-jre-jammy as builder
WORKDIR /app
COPY FactChecker-0.0.1-SNAPSHOT.jar app.jar

RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy layers from the builder stage
COPY --from=builder app/dependencies/ ./
COPY --from=builder app/spring-boot-loader/ ./
COPY --from=builder app/snapshot-dependencies/ ./
COPY --from=builder app/application/ ./

# Railway specific environment variable
ENV PORT=8080

# Expose the port
EXPOSE ${PORT}


# Start the application
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
