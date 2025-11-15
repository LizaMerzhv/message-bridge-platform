FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY mvnw ./mvnw
COPY .mvn .mvn
RUN chmod +x mvnw
COPY common/pom.xml common/pom.xml
COPY api/pom.xml api/pom.xml
COPY worker/pom.xml worker/pom.xml
COPY admin-ui/pom.xml admin-ui/pom.xml
COPY common/src common/src
COPY worker/src worker/src
RUN mvn -B -DskipTests -pl worker -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/worker/target/notifi-worker-*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
