FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /src

COPY pom.xml .

COPY src ./src

RUN mvn -B -DskipTests package


FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /src/target/*SNAPSHOT.jar app.jar

EXPOSE 8001

ENTRYPOINT ["java", "-jar", "app.jar"]
