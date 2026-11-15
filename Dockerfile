FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /app
COPY common common
RUN mvn -f common/pom.xml -q install -DskipTests
COPY ${SERVICE} ${SERVICE}
RUN mvn -f ${SERVICE}/pom.xml -q package -DskipTests

FROM eclipse-temurin:21-jre
ARG SERVICE
ARG PORT
WORKDIR /app
COPY --from=build /app/${SERVICE}/target/*.jar app.jar
EXPOSE ${PORT}
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]