# Build stage: compile and package the JAR with the Maven wrapper
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Dependencies first, so this layer is cached until pom.xml changes
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

# Run stage: only the Java runtime and the JAR
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
