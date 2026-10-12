FROM amazoncorretto:21-alpine
LABEL authors="Rom"
RUN apk add --no-cache curl
WORKDIR /app

COPY config-server/target/*.jar config-server.jar
COPY config-repo /app/config-repo
ENTRYPOINT ["java","-jar","/app/config-server.jar"]