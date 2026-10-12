FROM amazoncorretto:21-alpine
COPY target/*.jar user-service.jar
ENTRYPOINT ["java","-jar","/user-service.jar"]