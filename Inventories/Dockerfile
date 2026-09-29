# Etapa 1: compila el jar dentro de Docker. target/ esta en .gitignore, asi que en
# el servidor (clonado desde git) no existe el jar; tampoco hace falta Java ni Maven alli.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Primero solo el pom: las dependencias quedan en cache y no se descargan de nuevo
# mientras el pom no cambie.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Sin pruebas: contextLoads necesita la base de datos, que no existe durante el build.
RUN mvn -q -B -DskipTests package

# Etapa 2: imagen final solo con el JRE y el jar.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/Inventories-*.jar Inventories.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","Inventories.jar"]
