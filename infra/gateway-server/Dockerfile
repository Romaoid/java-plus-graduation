FROM amazoncorretto:21-alpine
LABEL authors="Rom"
RUN apk add --no-cache curl
WORKDIR /app

COPY gateway-server/target/*.jar gateway-server.jar
ENTRYPOINT ["java","-jar","/app/gateway-server.jar"]