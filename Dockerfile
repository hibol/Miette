FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app
COPY miette/.mvn/ .mvn
COPY miette/mvnw miette/pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline
COPY miette/src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre-jammy
# curl is required by the compose healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
