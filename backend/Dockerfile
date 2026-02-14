FROM maven:3.9-eclipse-temurin-17

WORKDIR /workspace
COPY . /workspace
RUN mvn -q -DskipTests package

EXPOSE 8080
CMD ["java", "-jar", "target/eventfinder-0.0.1-SNAPSHOT.jar"]
