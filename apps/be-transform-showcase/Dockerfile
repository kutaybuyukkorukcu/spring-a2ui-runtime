FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /workspace

# Copy reactor + module poms first (cache-friendly)
COPY pom.xml .
COPY packages/fogui-java-core/pom.xml packages/fogui-java-core/pom.xml
COPY packages/fogui-spring-boot-starter/pom.xml packages/fogui-spring-boot-starter/pom.xml
COPY packages/fogui-spring-web-starter/pom.xml packages/fogui-spring-web-starter/pom.xml
COPY apps/be-transform-showcase/pom.xml apps/be-transform-showcase/pom.xml

# Copy source trees and build backend with required modules
COPY packages/fogui-java-core/src packages/fogui-java-core/src
COPY packages/fogui-spring-boot-starter/src packages/fogui-spring-boot-starter/src
COPY packages/fogui-spring-web-starter/src packages/fogui-spring-web-starter/src
COPY apps/be-transform-showcase/src apps/be-transform-showcase/src
RUN mvn -q -DskipTests -pl apps/be-transform-showcase -am package

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=builder /workspace/apps/be-transform-showcase/target/*.jar app.jar

# Expose port
EXPOSE 5001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
    CMD wget -q --spider http://localhost:5001/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
