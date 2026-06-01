# Simply Study Booking System

Simply Study is a production-grade, highly optimized backend scheduling and booking platform. It enables Teachers to create courses, configure class section offerings, and schedule sessions, while allowing Parents to browse available slots and book class sections concurrently without scheduling conflicts.

---

## 🚀 Live Links & API Resources

- **Published Postman API Documentation**: [Simply Study Web Collection Documentation](https://documenter.getpostman.com/view/39898850/2sBXwnts5z)
- **Live Production Swagger UI**: [https://simply-study.onrender.com/swagger-ui/index.html](https://simply-study.onrender.com/swagger-ui/index.html)
- **Live Production OpenAPI Schema JSON**: [https://simply-study.onrender.com/v3/api-docs](https://simply-study.onrender.com/v3/api-docs)
- **Local Swagger UI Dashboard**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) *(When running your application locally)*

*Swagger UI is pre-configured with global parameters for headers (`UserId`, `X-User-Id`, and `Timezone`) to facilitate quick, interactive endpoint testing.*

---

## 🛠 Tech Stack

- **Core Framework**: Spring Boot 4.0.6 (Spring Web, Spring Data JPA, Jakarta Validation)
- **Database**: PostgreSQL (Production) / H2 (In-memory testing)
- **Java Platform**: OpenJDK 21
- **Boilerplate Reduction**: Project Lombok
- **API Documentation**: SpringDoc OpenAPI 3 / Swagger
- **Containerization**: Docker (Multi-stage build)

---

## 📋 Assumptions Made

1. **Header-Based Simulated Authentication**: For ease of evaluation and testing, role authentication is resolved via request headers (`UserId` or `X-User-Id`). In a production system, these would be extracted from verified JWT tokens.
2. **Offering-Level Booking**: Booking happens strictly at the offering level. Parents book the entire set of sessions representing that course section, and cannot book individual sessions independently.
3. **Calendar Integrity Constraints**: Conflict prevention operates per parent. While different parents can book overlapping offerings, a single parent is prevented from booking any offering whose sessions overlap with their existing booked sessions.
4. **Timezone Fallback**: When timezone header values are absent or invalid, the system defaults formatting to Coordinated Universal Time (`UTC`).
5. **No Double Bookings**: A parent cannot book the same course offering more than once (enforced by a database level unique constraint).

---

## 📖 API Documentation (Endpoints Reference)

Below is an overview of the REST API endpoints exposed by the service. All endpoints return a standard JSON envelope: `ApiResponse<T>`.

### Teacher API Endpoints
* **Create User (Teacher)**: `POST /api/users`
  * Body: `{"name": "Teacher Name", "email": "email@example.com", "role": "TEACHER", "timezone": "America/New_York"}`
* **Create Course**: `POST /api/courses`
  * Headers: `UserId` (Teacher ID)
  * Body: `{"title": "Java Basics", "description": "Learn Java"}`
* **Create Offering**: `POST /api/offerings`
  * Headers: `UserId` (Teacher ID)
  * Body: `{"courseId": 1, "title": "Summer Batch", "maxCapacity": 10}`
* **Add Sessions to Offering**: `POST /api/offerings/{offeringId}/sessions`
  * Headers: `UserId` (Teacher ID)
  * Body: `{"sessions": [{"startTime": "2026-06-15T09:00:00Z", "endTime": "2026-06-15T11:00:00Z"}]}`
* **Get Teacher Offerings**: `GET /api/offerings`
  * Headers: `UserId` (Teacher ID), `Timezone` (e.g. `America/New_York`)

### Parent API Endpoints
* **Create User (Parent)**: `POST /api/users`
  * Body: `{"name": "Parent Name", "email": "parent@example.com", "role": "PARENT", "timezone": "America/Chicago"}`
* **Get Available Offerings**: `GET /api/offerings`
  * Headers: `UserId` (Parent ID), `Timezone` (e.g. `America/Chicago`)
* **Book Offering**: `POST /api/bookings`
  * Headers: `UserId` (Parent ID)
  * Body: `{"offeringId": 1}`
* **Get Booked Offerings**: `GET /api/bookings`
  * Headers: `UserId` (Parent ID), `Timezone` (e.g. `America/Chicago`)

---

## 💾 Database Schema Overview

```
                   +-----------------------+
                   |         USERS         |
                   +-----------------------+
                   | PK | id (VARCHAR)     |
                   |    | name (VARCHAR)   |
                   |    | email (VARCHAR)* |  *Unique
                   |    | role (VARCHAR)   |  *TEACHER / PARENT
                   |    | timezone (VARCHAR)|
                   +-----------------------+
                               |
            +------------------+------------------+
            | 1                                   | 1
            |                                     |
            v 0..*                                v 0..*
+-----------------------+             +-----------------------+
|        COURSE         |             |        BOOKING        |
+-----------------------+             +-----------------------+
| PK | id (BIGINT)      |             | PK | id (BIGINT)      |
| FK | created_by (VAR) |             | FK | offering_id (BIG)|
|    | title (VARCHAR)  |             | FK | parent_id (VAR)  |
|    | description (VAR)|             |    | status (VARCHAR) |
+-----------------------+             |    | booked_at (TZ)   |
            |                         +-----------------------+
            | 1                                   | 1
            |                                     |
            v 0..*                                v 0..*
+---------------------------+         +-----------------------+
|         OFFERING          |         |    BOOKING_SESSION    |
+---------------------------+         +-----------------------+
| PK | id (BIGINT)          |         | PK | id (BIGINT)      |
| FK | course_id (BIGINT)   | <-------| FK | booking_id (BIG) |
| FK | teacher_id (VARCHAR) |         | FK | session_id (BIG) |
|    | title (VARCHAR)      |         +-----------------------+
|    | timezone (VARCHAR)   |                     ^
|    | max_capacity (INT)   |                     | 0..*
|    | current_enrollment   |                     |
|    | status (VARCHAR)     |                     |
|    | version (BIGINT)*    | *Optimistic Lock    |
+---------------------------+                     |
            |                                     |
            | 1                                   |
            |                                     |
            v 0..*                                | 1
+---------------------------+                     |
|          SESSION          |---------------------+
+---------------------------+
| PK | id (BIGINT)          |
| FK | offering_id (BIGINT) |
|    | start_time (TZ)      |
|    | end_time (TZ)        |
+---------------------------+
```

### Unique Constraints & Database Indexes
1. **User Identity Isolation**: `users.email` is protected by a unique database constraint.
2. **Double Booking Prevention**: Unique constraint `uq_parent_offering` on `booking(parent_id, offering_id)` ensures a parent cannot book the same course offering multiple times.
3. **Database Performance Indexing**:
   - `idx_session_time_range` on `session(start_time, end_time)` speeds up overlap queries.
   - Foreign key indexes (`idx_offering_teacher_id`, `idx_booking_parent_id`, etc.) ensure fast joins.

---

## 🔒 Concurrency & Conflict Handling Approach

1. **Optimistic Locking**:
   - The `Offering` entity uses a `@Version` field. During high-concurrency booking requests, Hibernate increments the version.
   - If two concurrent threads attempt to enroll a parent into the same offering, one request commits successfully, while the other throws an `OptimisticLockingFailureException`. The system intercepts this conflict to prevent over-enrollment beyond `maxCapacity`.
2. **Conflict Overlap Check**:
   - Before confirming a booking, the system runs an atomic, database-level query that checks if the parent has *any* active booking whose sessions overlap with the sessions of the new offering:
     $$\text{NewSession.startTime} < \text{ExistingSession.endTime} \quad \text{AND} \quad \text{NewSession.endTime} > \text{ExistingSession.startTime}$$
   - Overlaps are detected at the database query layer rather than loading collections into memory, preserving speed and transaction isolation.

---

## 🌍 Timezone Handling Approach

* **Database Storage**: All session timestamps are persisted in the PostgreSQL database using the `TIMESTAMP WITH TIME ZONE` type. The JVM reads and writes all Instants normalized to UTC.
* **On-the-fly Context Shifting**: 
  - Dynamic timezone representation is computed during read operations.
  - When fetching offerings or parent bookings, the API resolves the user's requested timezone from either the `Timezone` request header or a query parameter (falling back to UTC).
  - All session times are shifted into that local timezone before serializing the response payload to the client.

---

## 🔑 Environment Variables Required

Configure these environment variables in your deployment environment (e.g., Render) or your local shell:

| Environment Variable | Description | Default Value (Fallback) |
|---|---|---|
| `PORT` | The port on which the web server listens. | `8080` |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL. | `jdbc:postgresql://localhost:5432/study` |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL database username. | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL database password. | `project` |

---

## ⚙️ Steps to Run the Application Locally

### Prerequisites
* JDK 21 installed.
* PostgreSQL running locally with a database named `study`.

### Step 1: Clone and Configure Database
Ensure a local PostgreSQL database instance is running and matches the credentials in table above (or update your environment variables accordingly).

### Step 2: Build the Application
Compile the codebase and run the unit/integration tests to ensure compatibility:
```bash
./mvnw clean install
```

### Step 3: Run the Application
Execute the Spring Boot executable using the Maven wrapper:
```bash
./mvnw spring-boot:run
```
The application will start, by default binding to `http://localhost:8080`.

---

## 🐳 Docker Deployment (Render-Ready)

Build and run the containerized application locally using the provided Dockerfile:

```bash
# Build the Docker image
docker build -t simply-study-app .

# Run the container (binding container port 8080 to host port 8080)
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/study \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=project \
  simply-study-app
```
