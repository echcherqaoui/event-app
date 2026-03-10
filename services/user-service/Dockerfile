FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-privileged user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]