# 🎓 AcadeMap — Student Target-Based Learning & Guidance System

A full-stack web application for engineering students to set a target CGPA, track semester-wise GPA, and receive intelligent academic guidance across all 8 semesters.

---

## 🏗️ Tech Stack

| Layer     | Technology                              |
|-----------|-----------------------------------------|
| Frontend  | HTML5, CSS3, Vanilla JavaScript         |
| Backend   | Spring Boot 3.2 (REST API)             |
| ORM       | Hibernate (via Spring Data JPA)         |
| Database  | MySQL 8.x                               |
| Build     | Maven                                   |

---

## 📁 Project Structure

```
cgpa-tracker/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/cgpatracker/
│       │   ├── CgpaTrackerApplication.java     # Main class
│       │   ├── config/
│       │   │   └── GlobalExceptionHandler.java # REST error handling
│       │   ├── controller/
│       │   │   └── StudentController.java      # REST endpoints
│       │   ├── dto/
│       │   │   ├── ApiResponse.java            # Generic wrapper
│       │   │   ├── StudentDTO.java             # Student request/response
│       │   │   └── SemesterDTO.java            # Semester request/response
│       │   ├── model/
│       │   │   ├── Student.java                # Student entity
│       │   │   └── SemesterRecord.java         # Semester entity
│       │   ├── repository/
│       │   │   ├── StudentRepository.java      # JPA queries
│       │   │   └── SemesterRecordRepository.java
│       │   └── service/
│       │       └── StudentService.java         # Core business + CGPA logic
│       └── resources/
│           ├── application.properties          # DB config
│           └── schema.sql                      # DB setup script
└── frontend/
    ├── index.html                              # Main UI
    ├── style.css                               # Styles
    └── app.js                                  # API + logic
```

---

## 🚀 Setup Instructions

### Step 1 — MySQL Setup

```sql
-- Run schema.sql in MySQL Workbench or terminal:
mysql -u root -p < backend/src/main/resources/schema.sql
```

Or manually:
```sql
CREATE DATABASE cgpa_tracker_db CHARACTER SET utf8mb4;
```

### Step 2 — Configure Database

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cgpa_tracker_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD_HERE
```

### Step 3 — Run Spring Boot Backend

```bash
cd backend
mvn spring-boot:run
```

Backend starts at: `http://localhost:8080/api`

### Step 4 — Run Frontend

Open `frontend/index.html` directly in a browser,  
**OR** serve it with a simple server:

```bash
cd frontend
npx serve .
# OR
python3 -m http.server 3000
```

Then visit: `http://localhost:3000`

---

## 🔌 REST API Endpoints

| Method | Endpoint                                    | Description              |
|--------|---------------------------------------------|--------------------------|
| POST   | `/api/students`                             | Register new student     |
| GET    | `/api/students`                             | Get all students         |
| GET    | `/api/students/{id}`                        | Get student by ID        |
| POST   | `/api/students/{id}/semesters`              | Add semester GPA         |
| PUT    | `/api/students/{id}/semesters/{semNum}`     | Update semester GPA      |

### Sample Requests

**Register Student:**
```json
POST /api/students
{
  "name": "Prakash Kumar",
  "email": "prakash@college.edu",
  "registerNumber": "CSE2021001",
  "branch": "Computer Science Engineering",
  "targetCgpa": 9.5
}
```

**Add Semester GPA:**
```json
POST /api/students/1/semesters
{
  "semesterNumber": 1,
  "gpa": 9.2
}
```

---

## 🧠 Guidance Engine Logic

The system computes **required GPA per remaining semester**:

```
requiredGpa = (targetCgpa × 8 − currentCgpa × completedSems) / remainingSems
```

| Status      | Condition                                |
|-------------|------------------------------------------|
| 🌟 AHEAD    | currentCgpa ≥ targetCgpa                |
| ✅ ON_TRACK | requiredGpa ≤ 8.5                       |
| ⚠️ BEHIND   | 8.5 < requiredGpa ≤ 9.5               |
| 🚨 CRITICAL  | requiredGpa > 9.5                       |
| 🎓 ACHIEVED  | All 8 semesters done & target met       |

---

## 🎨 Frontend Features

- Dark industrial-academic design with amber accent
- Live CGPA progress bar with target marker
- 8-semester card grid with color-coded status
- Dynamic guidance banner per student state
- Real-time student leaderboard on dashboard
- CGPA slider + number input synchronisation
- Toast notifications for all actions
- Fully responsive mobile layout

---

## 📊 Database Schema

```sql
students (id, name, email, register_number, branch, target_cgpa, current_cgpa, current_semester, created_at, updated_at)

semester_records (id, student_id, semester_number, gpa, cgpa_after_semester, required_gpa_remaining, progress_status, guidance_message, recorded_at)
```

---

## ⚙️ CORS Configuration

The controller uses `@CrossOrigin(origins = "*")` for development.  
For production, restrict to your frontend domain:

```java
@CrossOrigin(origins = "https://yourdomain.com")
```

---

## 🔧 Lombok Setup

Ensure Lombok annotation processing is enabled in your IDE:
- **IntelliJ IDEA**: Settings → Build → Compiler → Annotation Processors → Enable
- **Eclipse**: Install Lombok plugin from `https://projectlombok.org/setup/eclipse`
