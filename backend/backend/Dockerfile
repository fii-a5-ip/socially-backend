# ==========================================
# STAGE 1: Builder-ul (Compilează aplicația)
# ==========================================
FROM maven:3.9.4-eclipse-temurin-17 AS builder

WORKDIR /build

# Copiem pom.xml și descărcăm dependențele
COPY pom.xml .

# Copiem sursele și compilăm proiectul generând acel folder "target" în interiorul Docker
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Runner-ul (Imaginea finală pe care o trimitem pe AWS)
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Acum merge! Tragem fișierul .jar din primul stagiu (builder)
COPY --from=builder /build/target/*.jar app.jar

# Expunem portul
EXPOSE 8080

# Pornim aplicația
ENTRYPOINT ["java", "-jar", "app.jar"]