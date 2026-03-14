FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY mvnw ./mvnw
COPY .mvn .mvn
RUN chmod +x mvnw
COPY common/pom.xml common/pom.xml
COPY api/pom.xml api/pom.xml
COPY worker/pom.xml worker/pom.xml
COPY security-service/pom.xml security-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY api-gateway/src api-gateway/src
RUN mvn -B -DskipTests -pl api-gateway -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/notifi-api-gateway-*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
