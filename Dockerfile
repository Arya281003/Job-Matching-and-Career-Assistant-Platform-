FROM eclipse-temurin:23-jdk AS build
WORKDIR /app

RUN apt-get update && apt-get install -y maven && apt-get clean

COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

COPY backend/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:23-jre
WORKDIR /app
COPY --from=build /app/target/jobmatch-backend-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]