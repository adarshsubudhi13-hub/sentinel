FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
COPY web ./web
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/sentinel-1.0.jar app.jar
COPY --from=build /app/web ./web
EXPOSE 8080
ENV PORT=8080
CMD ["java", "-jar", "app.jar"]
