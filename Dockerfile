# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/watchusee-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Xmx800m -jar app.jar --server.port=${PORT:-8080}"]