FROM eclipse-temurin:17-jre
ARG JAR_FILE=target/Inventories-0.0.1.jar
COPY ${JAR_FILE} Inventories.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","Inventories.jar"]