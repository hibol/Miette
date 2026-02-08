FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY miette/.mvn/ .mvn
COPY miette/mvnw miette/pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline
COPY miette/src ./src
RUN ./mvnw clean package -DskipTests
EXPOSE 8080
CMD ["./mvnw", "spring-boot:run"]