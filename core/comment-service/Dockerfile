FROM amazoncorretto:21-alpine
COPY target/*.jar comment-service.jar
ENTRYPOINT ["java","-jar","/comment-service.jar"]