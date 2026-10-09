FROM eclipse-temurin:21-jdk
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn -q -DskipTests package

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/campushire-backend-0.0.1-SNAPSHOT.jar"]
