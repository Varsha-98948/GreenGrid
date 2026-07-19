FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

# The image is intended for Railway/Render. Environment variables supplied by
# those platforms are resolved by application-prod.yml.
ENV SPRING_PROFILES_ACTIVE=prod

COPY --from=build /app/target/greengrid-backend-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
