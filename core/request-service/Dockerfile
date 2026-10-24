FROM amazoncorretto:21-alpine
COPY target/*.jar request-service.jar
ENTRYPOINT ["java","-jar","/request-service.jar"]