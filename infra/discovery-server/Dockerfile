FROM amazoncorretto:21-alpine
LABEL authors="Rom"
RUN apk add --no-cache curl
WORKDIR /app

COPY discovery-server/target/*.jar discovery-server.jar
ENTRYPOINT ["java","-jar","/app/discovery-server.jar"]