# Socially Backend — Ghid de Dezvoltare și Integrare

Acesta este repository-ul principal pentru backend-ul proiectului **Socially**, construit cu Spring Boot.

## 🚀 Tehnologii
- **Java 21** & **Spring Boot 3.x**
- **Spring Security** (JWT Stateless Auth)
- **Spring Data JPA** (Hibernate)
- **MySQL / TiDB Cloud** (Bază de date)
- **Maven** (Build Tool)

---

## 🛠️ Configurare Locală

### 1. Baza de Date
Asigură-te că ai acces la instanța de TiDB. String-ul de conexiune trebuie să includă SSL pentru a funcționa cu TiDB Cloud:
```properties
spring.datasource.url=jdbc:mysql://<host>:4000/socially?useSSL=true&sslMode=VERIFY_IDENTITY
```

### 2. Pornirea aplicației
```bash
cd backend/backend
./mvnw spring-boot:run
```

### 3. Rularea Testelor (SonarQube Ready)
Pentru a trece de Quality Gate, noul cod trebuie să aibă o acoperire de minim **80%**.
```bash
cd backend/backend
./mvnw clean test
```
*Dacă build-ul este "SUCCESS" local, atunci va trece și pe GitHub.*

---

## 🛡️ Securitate și Autentificare

### Regula de Aur (Whitelist)
Orice rută nouă adăugată într-un Controller este **BLOCATĂ implicit** (403 Forbidden). Pentru a permite accesul public, adaugă ruta în `SecurityConfig.java`:
```java
.requestMatchers("/api/public/**").permitAll()
```

### Obținerea User-ului Curent
Backend-ul extrage ID-ul automat din token-ul JWT. Folosim o abordare robustă care acceptă atât `String` cât și `UserDetails`:
```java
@GetMapping("/profile")
public ResponseEntity<?> getProfile(@AuthenticationPrincipal Object principal) {
    String userIdStr = (principal instanceof UserDetails) 
        ? ((UserDetails) principal).getUsername() 
        : principal.toString();
    Integer userId = Integer.parseInt(userIdStr);
    // ...
}
```

### CORS
CORS este configurat centralizat în `SecurityConfig.java`. În producție, adaugă URL-ul de Vercel în lista `allowedOrigins`.

---

## 📐 Arhitectură și Pattern-uri

Respectăm fluxul: **Controller → Service → Mapper → Repository**.

1. **Events vs Outgoings**: Folosim exclusiv termenul **Events** pentru evenimente/ieșiri. Rutele vechi `/api/outgoings` sunt acum `/api/events`.
2. **DTO-uri**: Folosește DTO-urile din pachetul `com.soccialy.backend.dto`.
3. **Mappere**: Folosește componentele din pachetul `mapper` pentru conversii.
4. **Erori**: Folosește `ResourceNotFoundException` pentru resurse inexistente (automat returnează 404).

---

## 🌐 Integrare Frontend

### Variabile de Mediu
Frontend-ul trebuie să folosească o variabilă de mediu pentru URL-ul API:
- Local: `http://localhost:8080`
- Producție: URL-ul de deploy (Render/AWS/Railway)

### Exemplu Request (Create Group)
- **Endpoint**: `POST /api/groups`
- **Header**: `Authorization: Bearer <jwt_token>` (dacă ruta e protejată)
- **Body**:
```json
{
  "name": "Nume Grup",
  "imgLink": "base64_sau_url",
  "creatorUserId": 1,
  "memberIds": [2, 3, 4]
}
```
### Exemple Endpoint-uri (Update)

#### 1. Căutare și Sortare Evenimente (AI Powered)
- **Endpoint**: `GET /api/events/search`
- **Params**: `query` (string, obligatoriu), `maxDistance` (default 50), `maxDays` (default 30)
- **Descriere**: Returnează evenimente sortate după relevanță (filtre utilizator + AI matching).
