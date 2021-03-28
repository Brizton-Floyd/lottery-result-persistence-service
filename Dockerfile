FROM openjdk:8
ADD lottery-result-persistence-server/target/result-persistence-service.jar result-persistence-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "result-persistence-service.jar"]

#lottery-result-persistence-server/target/result-persistence-service.jar