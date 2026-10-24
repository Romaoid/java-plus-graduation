FROM amazoncorretto:21-alpine
COPY target/*.jar event-service.jar
ENTRYPOINT ["java","-jar","/event-service.jar"]