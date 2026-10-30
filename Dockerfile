# One Dockerfile for every service:  docker build --build-arg MODULE=booking-service .
FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /app
COPY . .
RUN mvn -q -B -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre
ARG MODULE
WORKDIR /app
COPY --from=build /app/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
