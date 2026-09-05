# Build stage: compiles the Kotlin/Maven project into an executable jar.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy only the files needed to resolve dependencies first, so Docker can
# cache this layer and skip re-downloading dependencies when only source
# code changes (not pom.xml).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Now copy the actual source and build the jar.
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Run stage: a minimal JRE-only image, no build tools included, to keep the
# final image small.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/personalfinancemanager-0.0.1-SNAPSHOT.jar app.jar

# Render sets the PORT environment variable at runtime; application.properties
# reads it via server.port=${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
