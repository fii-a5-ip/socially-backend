# 1. Folosim o imagine oficială și minimală de Java 17 (JRE e suficient pt rulare)
FROM eclipse-temurin:17-jre-alpine

# 2. Setăm directorul de lucru în interiorul containerului
WORKDIR /app

# 3. Copiem fișierul .jar generat de Maven din folderul target în container
# Notă: Spring Boot generează de obicei un singur executabil .jar aici
COPY target/*.jar app.jar

# 4. Expunem portul pe care rulează aplicația (8080 by default la Spring)
EXPOSE 8080

# 5. Comanda care pornește efectiv aplicația când containerul este lansat
ENTRYPOINT ["java", "-jar", "app.jar"]