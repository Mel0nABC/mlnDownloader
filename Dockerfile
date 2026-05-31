## BUILDER IMAGE

FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn package -DskipTests


## APPLICATION RUNNING IMAGE


FROM eclipse-temurin:25.0.3_9-jdk-ubi10-minimal

COPY --from=build /app/target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]