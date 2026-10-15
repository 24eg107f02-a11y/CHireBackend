# CampusHire

CampusHire is a college placement management portal that connects students, recruiters, and placement officers through a React frontend and Spring Boot API.

## Prerequisites

- JDK 25 (the backend Maven configuration targets Java 25)
- Maven 3.9 or later
- MySQL 8.0 or later
- Node.js 20.19+ or 22.12+ and npm (required by Vite 7)
- Docker Desktop with Docker Compose v2 (for the containerized single-host setup below)

The project is configured to compile for Java 25. A compatible JDK 25 or newer is required.

## Set up the database

1. Start the MySQL service.
2. Connect with an account that can create databases and run:

   ```powershell
   Get-Content -Raw .\database\schema.sql | mysql -u root -p
   ```

   This creates the `campushire` database if it does not already exist. You can also create it in MySQL Workbench by running the SQL in `database/schema.sql`.

## Configure and run the backend

Open a PowerShell terminal in the `backend` folder. Set the database account for that terminal session; the password is read without displaying it:

```powershell
$env:SERVER_PORT = "8081"
$env:DB_USERNAME = "root"
$securePassword = Read-Host "MySQL password" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $securePassword).Password
$jwtBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
mvn spring-boot:run
```

For this workspace's demo database, an easier option is to open PowerShell at the project root and run `.\backend\start-local.ps1`. The script prompts for the MySQL username and password without saving either, uses the `campushire_demo` database, selects port 8081, generates a temporary JWT key, and starts Spring Boot. Keep that terminal open while using the frontend.

If your MySQL account has no password, set `$env:DB_PASSWORD = ""` instead of prompting. A JWT signing key of at least 256 bits is required. The command above makes a random key for local development; keep it private and use the same value in each backend terminal where existing tokens should remain valid. Do not commit database credentials or JWT keys to source control.

To provision the placement officer account on first startup, set these optional values in the same PowerShell session before `mvn spring-boot:run`:

```powershell
$env:OFFICER_EMAIL = "officer@your-college.edu"
$officerPassword = Read-Host "New officer password (at least 12 characters)" -AsSecureString
$env:OFFICER_PASSWORD = [System.Net.NetworkCredential]::new("", $officerPassword).Password
```

Officer provisioning is skipped when both variables are unset. Supplying only one is an error. Public registration does not allow the `PLACEMENT_OFFICER` role. Once an officer already exists at that address, startup does not change its password.

This workspace already has another process using port 8080, so the commands above run CampusHire on `http://localhost:8081`. On a machine where port 8080 is free, omit `SERVER_PORT` to use the default. Check application and database health at:

```text
http://localhost:8081/actuator/health
```

A healthy response confirms that Spring Boot can reach the configured database. If the connection fails, verify that MySQL is running, the database exists, and `DB_USERNAME`/`DB_PASSWORD` match your local MySQL account. You can override the connection URL with `DB_URL`.

Flyway runs before Hibernate starts. The migration set creates the application tables on a new database and safely adds missing audit timestamp columns to existing CampusHire tables, backfilling those new columns for old rows. It does not drop tables or delete records. Back up an existing database before starting the updated backend. Local development keeps `spring.jpa.hibernate.ddl-auto=update`; the production profile validates the migrated schema instead. The `database/schema.sql` script creates the database itself.

To load demonstration companies and open job listings, first start the backend once so Flyway creates the tables, then register a recruiter account with `demo.recruiter@example.edu`. Run the idempotent seed script from the project root, specifying the database that the backend uses:

```powershell
Get-Content -Raw .\database\sample-data.sql | mysql -u root -p campushire
```

The script adds three already-approved demo jobs so they appear in public browsing and recommendations. It does not create a user or set a password. Run it again safely to display the existing seeded jobs without duplicating them. If you followed the separate-database path used for this workspace because its older `campushire` schema was incompatible, use `campushire_demo` instead of `campushire` in the command above.

## Authentication API

- `POST /api/auth/register` — create a student or recruiter account; stores a BCrypt password hash and returns a short-lived JWT
- `POST /api/auth/login` — verify credentials and issue a JWT
- `GET /api/auth/me` — return the authenticated user's public identity; requires `Authorization: Bearer <token>`

Unauthenticated job browsing remains public. `/api/students/**`, `/api/recruiters/**`, and `/api/officer/**` are restricted to their respective roles. Other `/api/**` endpoints require a valid JWT. Placement officer accounts are provisioned only through the optional server-side environment variables above.

## Student API

Use `Authorization: Bearer <token>` from a student's login or registration response:

- `GET /api/students/profile` — read the signed-in student's profile
- `PUT /api/students/profile` — update that student's profile
- `POST /api/students/jobs/{jobId}/applications` — apply for an approved, unexpired job when branch, CGPA, and required skills match
- `GET /api/students/applications` — list the signed-in student's applications and statuses
- `GET /api/students/interviews` — list the signed-in student's scheduled interviews
- `GET /api/students/recommendations` — rank up to eight open jobs by a transparent branch, CGPA, and skills match; response includes an independent eligibility result and reasons
- `GET /api/jobs?company=acme&location=remote&branch=Computer%20Science&cgpa=8.0` — browse public approved jobs with optional filters

Skills are stored as comma-, semicolon-, or newline-separated text in the profile. The profile update uses `PUT`: optional fields omitted from the request are cleared. Applications are unique per student and job; duplicate applications return HTTP 409, and failed eligibility checks return HTTP 422 with reasons.

## Run the frontend

Open a second PowerShell terminal in the `frontend` folder:

```powershell
npm install
Copy-Item .env.example .env
npm run dev
```

Open the local URL printed by Vite (normally `http://localhost:5173`). The frontend calls the backend at `http://localhost:8080` by default. Since this workspace runs the backend on port 8081, set `VITE_API_URL` in `frontend/.env`:

```dotenv
VITE_API_URL=http://localhost:8081
```

Keep the backend running in its own terminal. Vite is configured to allow local development origins through the backend CORS configuration. Build the production frontend with `npm run build`.

## Deploy the frontend and API to Netlify and Render

The root `render.yaml` defines a Docker web service for the Spring Boot API. Connect this repository in Render and create a Blueprint from that file. Render will generate `JWT_SECRET`; when prompted for the unsynced environment variables, provide:

- `DB_URL` — JDBC URL for a reachable hosted MySQL database, including the database name and required connection options
- `DB_USERNAME` and `DB_PASSWORD` — credentials for that database

The backend uses MySQL and this Blueprint does not provision a database. Do not put database credentials or secrets in `render.yaml` or commit them to source control. Render assigns the service URL after creation; verify it responds at `/actuator/health/readiness`.

The root `netlify.toml` configures the existing frontend build from `frontend/`. In the Netlify site's environment variable settings, set `VITE_API_URL` to the Render service's HTTPS origin (for example, `https://campushire-api.onrender.com`, without a trailing slash), then trigger a new deploy. The Render Blueprint already restricts browser API access to `https://24eg107f02.netlify.app`; if the Netlify site's domain changes, update `CORS_ALLOWED_ORIGINS` in Render to the new origin and redeploy the API.

The Render Blueprint uses the free web-service plan, which may spin down when idle. This setup does not create a MySQL database or push code to either hosting provider; the repository must be connected to Render and Netlify, and the database must be hosted separately.

## Containerized deployment package (deployment not performed)

The repository includes a Docker Compose setup for running the React site, Spring Boot API, and MySQL together. MySQL data is stored in a named volume and its port is not published to the host. The API runs with the `prod` Spring profile, applies Flyway migrations, validates the schema, and is not directly published; Nginx serves the frontend and proxies API requests. The frontend is available at `http://localhost:8088` by default.

From the project root, make a private environment file and set strong unique secrets:

```powershell
Copy-Item .env.example .env
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

Use the generated value for `JWT_SECRET`, and set unique strong values for `MYSQL_ROOT_PASSWORD` and `DB_PASSWORD` in `.env`. Do not commit or share `.env`. If you need a placement officer, also set `OFFICER_EMAIL` and a strong `OFFICER_PASSWORD` (at least 12 characters); remove the officer password from the deployment environment after the account is provisioned.

Build and start the local containers:

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f backend
```

Open `http://localhost:8088`. Check backend readiness with `docker compose exec backend curl --fail http://localhost:8080/actuator/health/readiness`; the backend health endpoint is private to the Docker network. Stop the containers with `docker compose down`; the named database volume is retained. Do not remove the volume if it contains data you need. Change `APP_PORT` in `.env` if port 8088 is already in use. To serve traffic publicly, put the site behind a TLS-terminating reverse proxy or managed ingress, configure firewall rules and backups, and set `CORS_ALLOWED_ORIGINS` only if the browser frontend is hosted on a different origin. The Compose file is a deployment baseline, not a substitute for production TLS, monitored backups, or managed secret storage.

For a fresh MySQL database, Flyway applies the schema-creation migration before Hibernate validates it. Existing databases are not reset. Back them up before startup; the legacy audit-column migration only adds missing columns and backfills their values.

## Recruiter API

Use a recruiter JWT (`Authorization: Bearer <token>`) for these endpoints:

- `GET /api/recruiters/companies` — list your companies
- `POST /api/recruiters/companies` — create a company
- `PUT /api/recruiters/companies/{companyId}` and `DELETE /api/recruiters/companies/{companyId}` — edit or delete your company; companies with jobs cannot be deleted
- `GET /api/recruiters/jobs` — list your jobs
- `POST /api/recruiters/jobs` and `PUT /api/recruiters/jobs/{jobId}` — create or edit your jobs
- `POST /api/recruiters/jobs/{jobId}/close` and `DELETE /api/recruiters/jobs/{jobId}` — close a job or delete it if it has no applications
- `GET /api/recruiters/jobs/{jobId}/applications` — review applicants for your job
- `PATCH /api/recruiters/applications/{applicationId}/status` — shortlist, reject, or select an applicant
- `POST /api/recruiters/applications/{applicationId}/interviews` — schedule an interview for a shortlisted applicant

New jobs start in `PENDING_APPROVAL`; editing an approved job returns it to that state. Recruiters cannot approve their own jobs. Application status follows `APPLIED → SHORTLISTED → SELECTED` or `APPLIED/SHORTLISTED → REJECTED`; selected and rejected applications cannot be reopened. A recruiter can only manage companies, jobs, applicants, and interviews connected to their own account.

## Placement officer API

Use a provisioned placement-officer JWT for these endpoints:

- `GET /api/officer/dashboard` — user, company, job, application, and placement-drive totals
- `GET /api/officer/users?role=STUDENT` — list users, optionally filtered by role; responses omit password fields
- `PATCH /api/officer/users/{userId}/status` — enable or disable an account; an officer cannot disable themself or the last enabled officer
- `GET /api/officer/jobs?status=PENDING_APPROVAL` — list jobs awaiting review (pending approval is the default)
- `POST /api/officer/jobs/{jobId}/approve` and `POST /api/officer/jobs/{jobId}/reject` — record a review and optional notes; only pending jobs can be reviewed
- `GET /api/officer/drives`, `POST /api/officer/drives`, `PUT /api/officer/drives/{driveId}`, `DELETE /api/officer/drives/{driveId}` — manage placement drives; date ranges are validated and drives assigned to a job cannot be deleted
- `PUT /api/officer/jobs/{jobId}/placement-drive` — assign a drive using `{"placementDriveId": 1}`; send `{"placementDriveId": null}` to remove the assignment
- `GET /api/officer/reports/placements` — selected-application totals and selected students grouped by company

## Placement officer bootstrap

Provision the first placement officer by setting `OFFICER_EMAIL` and `OFFICER_PASSWORD` in the backend environment before startup, as shown in the configuration section. Public registration cannot create officer accounts. A valid officer account is required to access officer APIs.

Example registration request:

```json
{
  "fullName": "Alex Student",
  "email": "alex@example.com",
  "password": "choose-a-strong-password",
  "role": "STUDENT"
}
```

Run the backend build and test lifecycle from the `backend` folder with:

```powershell
mvn test
```

The current repository tests use a temporary in-memory H2 database, so they do not need MySQL credentials. They check that public job search returns only approved, unexpired jobs and applies its filters.

## Project layout

- `backend/` — Spring Boot API, Java 25 target, JPA, validation, Flyway schema migrations, MySQL driver, production profile, and health endpoint
- `frontend/` — React/Vite app with React Router, Axios, role-aware dashboards, responsive styles, and Nginx container configuration
- `docker-compose.yml` — frontend, API, and persistent MySQL services for single-host deployment
- `backend/src/main/java/com/campushire/entity/` — normalized placement domain entities and relationships
- `backend/src/main/java/com/campushire/repository/` — Spring Data JPA repositories
- `backend/src/main/java/com/campushire/dto/` — API request and response shapes
- `backend/src/main/java/com/campushire/controller/` — REST controllers
- `backend/src/main/java/com/campushire/service/` — application services
- `database/schema.sql` — initial database creation script
- `database/sample-data.sql` — repeatable demo company and approved-job seed script
- `.env.example` — names and placeholders for container environment settings; copy to an untracked `.env` and replace all secret placeholders

## Implemented functionality and limitations

- Student/recruiter registration and login, BCrypt password hashing, JWT authentication, and role-protected APIs.
- Students can edit profiles, browse and apply to eligible approved jobs, track application statuses and interviews, and view explainable job recommendations. Recommendations use a rule-based branch/CGPA/skills match, not machine learning; eligibility is reported separately.
- Recruiters can create and edit their company profiles and job postings, close roles, review applicants, change application outcomes, and schedule interviews for shortlisted candidates.
- Placement officers can review job postings, enable/disable accounts, create/edit/delete placement drives, view dashboard metrics, and see placement reports.
- The frontend presents public job browsing and the role-specific dashboards. Some features are handled in combined dashboard pages rather than separate pages.
- Automated backend tests use an in-memory H2 database. A successful test run does not verify credentials or connectivity to a local MySQL server; confirm that separately through `/actuator/health`.
- The placement report summarizes selected applications reported by recruiters. This is not an independently verified placement outcome.

The included Compose configuration is intended as a basic single-host deployment starting point. It does not provide TLS termination, cloud secret management, managed backups, or external monitoring; configure those before exposing the service to public production traffic.

Run `mvn test` from `backend` and `npm run build` from `frontend` to verify the backend and frontend respectively. Build the container package with `docker compose build` on a machine with Docker Desktop or Docker Engine installed.
