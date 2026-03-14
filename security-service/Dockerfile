FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY mvnw ./mvnw
COPY .mvn .mvn
RUN chmod +x mvnw
COPY notification-api-service/pom.xml notification-api-service/pom.xml
COPY notification-delivery-service/pom.xml notification-delivery-service/pom.xml
COPY security-service/pom.xml security-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY admin-service/pom.xml admin-service/pom.xml
COPY security-service/src security-service/src
RUN mvn -B -DskipTests -pl security-service -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/security-service/target/notifi-security-service-*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
