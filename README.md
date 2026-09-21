# Reading Room: Library Management System

A full-stack library management system built in **core Java with no frameworks**. It has a custom HTTP server, a JSON REST API backed by **MySQL through JDBC**, and a plain HTML/CSS/JavaScript frontend.

The goal of the project is to show how the pieces of a web backend actually work: routing, authentication, data access and concurrency control. Everything a framework normally hides is written by hand, using only the JDK plus the MySQL JDBC driver.

## Features

- **Three roles.** Students borrow up to 3 items, teachers up to 6, and librarians manage the catalogue and member access.
- **Catalogue.** Books (14-day loans) and DVDs (5-day loans) with live copy counts.
- **Borrow and return.** Due dates are tracked and copies return to the shelf when returned.
- **Safe concurrent borrowing.** Per-item and per-user locks prevent double-booking the last copy and stop a user exceeding their limit with parallel requests.
- **Authentication.** Salted password hashing and token-based sessions. Deactivating an account takes effect immediately.
- **Librarian console.** Add, edit and remove items, and revoke or restore member access.
- **Frontend.** Homepage, sign in / sign up, member dashboard, librarian console and contact page. Responsive.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java (JDK 21) |
| HTTP server | `com.sun.net.httpserver.HttpServer` (built into the JDK) |
| Database | MySQL, accessed through JDBC (`PreparedStatement`, MySQL Connector/J) |
| Concurrency | `ExecutorService` thread pool, `ReentrantLock`, `ConcurrentHashMap` |
| Frontend | HTML, CSS, vanilla JavaScript (`fetch`) |

## Architecture

```
Browser (HTML/CSS/JS)
   |  HTTP + JSON, X-Auth-Token header
   v
HttpServer  --> fixed thread pool (16 worker threads)
   |
   +-- "/"    StaticFileHandler   serves the frontend
   +-- "/api" Router              dispatches "METHOD path" to a handler
                 |
                 v
              Handlers            parse the request, check the role, build the response
                 |
                 v
              Services            business rules: AuthService, ResourceManager, SessionManager
                 |
                 v
              Repositories        JDBC data access behind Repository<T, ID>
                 |
                 v
              MySQL
```

Domain failures are custom checked exceptions (`LibraryException` and its subtypes). The `Router` catches them in one place and maps them to HTTP status codes.

## Project structure

```
src/com/pranav/library/
  model/          User (abstract) -> Student, Teacher, Librarian
                  LibraryResource (abstract) -> Book, Dvd
                  BorrowRecord, Role
  exceptions/     LibraryException and its subtypes
  repository/     Repository<T, ID> interface + JDBC implementations
  service/        ResourceManager (concurrency core), AuthService, SessionManager
  http/           Router, RouteHandler, StaticFileHandler
  http/handlers/  AuthHandler, ResourceHandler, BorrowHandler, AdminHandler
  util/           HttpUtil, JsonUtil (hand-written), PasswordUtil, DbConnection
  Main.java       wires everything together and starts the server

frontend/         homepage.html, contact.html, index.html (sign in / up),
                  dashboard.html, admin.html, css/, js/
lib/              MySQL Connector/J
sql/              schema.sql, dummy_data.sql
```

## API reference

Protected routes need the header `X-Auth-Token: <token>` returned by login. Request and response bodies are JSON.

| Method | Path | Access | Purpose | Success |
|---|---|---|---|---|
| POST | `/api/auth/signup` | Public | Create a student or teacher account | 201 |
| POST | `/api/auth/login` | Public | Returns `{token, id, name, role}` | 200 |
| POST | `/api/auth/logout` | Logged in | Invalidate the token | 200 |
| GET | `/api/resources` | Logged in | List the catalogue | 200 |
| POST | `/api/resources` | Librarian | Add an item | 201 |
| PUT | `/api/resources?id=` | Librarian | Edit title, author or total copies | 200 |
| DELETE | `/api/resources?id=` | Librarian | Remove an item (refused while copies are on loan) | 200 |
| POST | `/api/borrow` | Student, teacher | Borrow `{resourceId}` | 201 |
| POST | `/api/return` | Student, teacher | Return `{resourceId}` | 200 |
| GET | `/api/my-borrows` | Logged in | The caller's loans | 200 |
| GET | `/api/admin/users` | Librarian | List members | 200 |
| PUT | `/api/admin/users/revoke?id=` | Librarian | Deactivate a member | 200 |
| PUT | `/api/admin/users/restore?id=` | Librarian | Reactivate a member | 200 |

**Status codes.** 400 bad or malformed input, 401 missing or invalid token, 403 wrong role, 404 unknown route or resource, 409 business-rule conflict (no copies left, borrow limit reached, already borrowed, duplicate email, item still on loan), 500 unexpected error.

## Concurrency design

Each request runs on its own worker thread, so two people can genuinely try to borrow the last copy at the same moment. `ResourceManager` prevents the check-then-act race with two kinds of lock:

- **A lock per item** protects the read, check and write of `availableCopies`. Borrowing different items never blocks.
- **A lock per user** protects the "has this user reached their limit?" check. Without it, one user sending parallel requests for different items would pass the check on every thread.

Locks are always taken in the same order (user, then item), and return, edit and remove take only the item lock, so no thread can wait on a user lock while holding an item lock. This rules out deadlock. If saving the borrow record fails, the copy count is restored.

**Verified by test.** 20 simultaneous borrow requests against an item with 3 copies produced exactly 3 successes (`201`) and 17 rejections (`409`), and `available_copies` ended at 0.

## Getting started

### Prerequisites

- JDK 21 or newer (`java -version`, `javac -version`)
- MySQL 8 running locally

MySQL Connector/J is already in `lib/`.

### 1. Create the database

```
mysql -u root -p < sql/schema.sql
mysql -u root -p library_db < sql/dummy_data.sql
```

If your MySQL is not on the default port 3306 (for example, XAMPP is using it), add `-h 127.0.0.1 -P <port>` to those commands.

### 2. Configure the connection

The server reads its settings from environment variables, so no password is stored in the code.

| Variable | Meaning | Default |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/library_db` |
| `DB_USER` | Database user | `root` |
| `DB_PASSWORD` | Database password | empty |
| `PORT` | Web server port | `8080` |
| `FRONTEND_DIR` | Frontend folder | `frontend` |

Windows (cmd):

```
set DB_PASSWORD=your_password
set DB_URL=jdbc:mysql://localhost:3307/library_db
```

Windows (PowerShell): `$env:DB_PASSWORD = "your_password"`. macOS and Linux: `export DB_PASSWORD=your_password`.

### 3. Compile and run

Run these from the project root (the folder containing `src`, `lib` and `frontend`).

Windows:

```
javac -cp "lib/*" -sourcepath src -d bin src\com\pranav\library\Main.java
java -cp "bin;lib/*" com.pranav.library.Main
```

macOS and Linux:

```
javac -cp "lib/*" -sourcepath src -d bin src/com/pranav/library/Main.java
java -cp "bin:lib/*" com.pranav.library.Main
```

You should see `Database connection OK` and `Library server running on http://localhost:8080`. Open **http://localhost:8080**. The frontend and the API are served by the same process, so there are no CORS issues.

### Demo accounts

Loaded by `sql/dummy_data.sql`. These are for local development only; change them before deploying anywhere.

| Role | Email | Password |
|---|---|---|
| Librarian | admin@library.com | admin123 |
| Student | asha.mehta@student.spu.edu | student123 |
| Student | rohan.iyer@student.spu.edu | student123 |
| Teacher | priya.kapoor@faculty.spu.edu | teacher123 |

Librarian accounts cannot be created through public signup; they are created directly in the database.

## Design decisions

- **Abstract `User` and `LibraryResource`, not a role flag.** Behaviour really differs by subtype (`getMaxBorrowLimit()` is 3, 6 or 0; `getBorrowDurationDays()` is 14 or 5), so polymorphism replaces `if (role == ...)` chains.
- **`HttpServer` instead of a micro-framework.** Routing, JSON handling and authentication are written by hand, to show what a framework does underneath.
- **Checked exceptions for domain errors.** Every failure has to be handled at the HTTP boundary and turned into the right status code, instead of surfacing as an unhandled 500.
- **Generic `Repository<T, ID>`.** Same contract shape as Spring Data, implemented by hand with `PreparedStatement` and `ResultSet`.
- **Parameterised queries everywhere**, so user input is never concatenated into SQL.

## Known limitations

- Passwords use a salted SHA-256 hash. A production system should use bcrypt or Argon2.
- Sessions live in server memory, so they are lost on restart and never expire.
- Each query opens a new database connection; there is no connection pool.
- Borrowing is protected by in-process locks, not a single database transaction, so the guarantee holds for one server instance only. Running several instances would need transactions or row-level locking (`SELECT ... FOR UPDATE`).
- There is no rate limiting on login.
- `Access-Control-Allow-Origin` is `*` for development convenience.

## Possible next steps

Connection pooling, database transactions for borrow and return, bcrypt hashing, session expiry, overdue and fine tracking, search and pagination, and a Spring Boot port for comparison.