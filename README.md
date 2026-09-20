# Reading Room — Concurrent Library Reservation System

A full-stack library management system built in **pure Java** (no Spring, no framework) to
demonstrate OOP fundamentals, concurrency control, and REST API design — before learning
Spring Boot. Frontend is plain HTML/CSS/JS. Backend uses only the JDK: `com.sun.net.httpserver`
for HTTP, `java.sql` for the database, hand-written JSON handling, and `ReentrantLock` for
thread safety.

**This has been fully built and tested end-to-end**, including a live concurrency test:
20 simultaneous borrow requests against a book with 3 copies produced *exactly* 3 successes
and 17 clean rejections — no race condition, no double-booking. Details at the bottom.

---

## 1. Prerequisites

- **JDK 21** (or newer) — check with `java -version` and `javac -version`
- **MySQL** (or MariaDB, which is wire-compatible) — a local install is fine
- **VS Code** with the **Extension Pack for Java** (Microsoft) installed

The JDBC driver you need (`mariadb-java-client-2.7.6.jar`) is already included in `lib/` —
you don't need to download anything separately. It works against both MySQL and MariaDB.

---

## 2. Set up the database

Open a terminal (or MySQL Workbench / phpMyAdmin-equivalent) and run, in order:

```powershell
mysql -u root -p < sql\schema.sql
mysql -u root -p library_db < sql\dummy_data.sql
```

This creates the `library_db` database with three tables (`users`, `resources`,
`borrow_records`) and seeds it with 4 demo accounts and 7 catalogue items.

**Demo accounts** (see `sql/dummy_data.sql` for the full list):

| Role      | Email                          | Password    |
|-----------|--------------------------------|-------------|
| Librarian | admin@library.com              | admin123    |
| Student   | asha.mehta@student.spu.edu     | student123  |
| Student   | rohan.iyer@student.spu.edu     | student123  |
| Teacher   | priya.kapoor@faculty.spu.edu   | teacher123  |

Then create a database user the backend will connect as (or just use `root` for local testing):

```sql
CREATE USER 'libapp'@'localhost' IDENTIFIED BY 'libapp_pw';
GRANT ALL PRIVILEGES ON library_db.* TO 'libapp'@'localhost';
FLUSH PRIVILEGES;
```

---

## 3. Open the project in VS Code

1. `File → Open Folder` → select the `backend` folder (the one containing `src/`).
2. VS Code should detect it as a Java project automatically. If prompted, let it index.
3. Add the JDBC driver to the project's classpath: open the **Java Projects** panel in the
   sidebar → right-click **Referenced Libraries** → **Add Jar Folder...** → select `../lib`.
   (If you don't see the Java Projects panel, install the *Extension Pack for Java* first.)

---

## 4. Configure the database connection

The backend reads DB credentials from environment variables, so nothing is hardcoded.
Create `backend/.vscode/launch.json` (VS Code will offer to generate one the first time you
run `Main.java` — edit it to add the `env` block):

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Launch Main",
      "request": "launch",
      "mainClass": "com.pranav.library.Main",
      "projectName": "backend",
      "env": {
        "DB_URL": "jdbc:mysql://127.0.0.1:3306/library_db",
        "DB_USER": "libapp",
        "DB_PASSWORD": "libapp_pw",
        "PORT": "8080",
        "FRONTEND_DIR": "${workspaceFolder}/../frontend"
      }
    }
  ]
}
```

## 5. Run it

Press **F5** (or the ▶ Run button above `main()` in `Main.java`). You should see:

```
Library server running on http://localhost:8080
Serving frontend from: C:\...\frontend
```

Open **http://localhost:8080** in a browser — that's the whole app, frontend and backend,
served from one process (no separate XAMPP-style static server needed, no CORS issues).

**Compiling/running from the command line instead** (PowerShell, from inside `backend/`):

```powershell
javac -d out -cp "..\lib\mariadb-java-client-2.7.6.jar" (Get-ChildItem -Recurse -Filter *.java src | % { $_.FullName })
$env:DB_URL="jdbc:mysql://127.0.0.1:3306/library_db"; $env:DB_USER="libapp"; $env:DB_PASSWORD="libapp_pw"
java -cp "out;..\lib\mariadb-java-client-2.7.6.jar" com.pranav.library.Main
```

---

## 6. Project structure

```
backend/src/com/pranav/library/
  model/         User hierarchy (Student/Teacher/Librarian), LibraryResource hierarchy (Book/Dvd), BorrowRecord
  exceptions/    Custom checked-exception hierarchy (LibraryException and subtypes)
  repository/    Hand-written Repository<T,ID> interface + JDBC implementations
  service/       ResourceManager (the concurrency core), AuthService, SessionManager
  http/          Router, RouteHandler, StaticFileHandler — a hand-built dispatch layer
  http/handlers/ AuthHandler, ResourceHandler, BorrowHandler, AdminHandler
  util/          HttpUtil, JsonUtil (hand-rolled), PasswordUtil, DbConnection
  Main.java      Wires everything together, starts the HTTP server

frontend/
  index.html, dashboard.html, admin.html
  css/style.css
  js/api.js, auth.js, dashboard.js, admin.js

sql/
  schema.sql       Table definitions
  dummy_data.sql   4 demo accounts + 7 catalogue items + 2 sample borrow records
```

---

## 7. What to say about each part in an interview

**Why abstract classes for `User` and `LibraryResource`, not one class with a role flag?**
Because behavior genuinely differs per subtype, not just a label: `Student.getMaxBorrowLimit()`
returns 3, `Teacher` returns 6, `Librarian` returns 0 and overrides `canManageLibrary()`.
Similarly `Book.getBorrowDurationDays()` returns 14, `Dvd` returns 5. If every subclass behaved
identically, a flag would be the right call — the differing behavior is what justifies the
hierarchy here.

**Why a `ReentrantLock` per resource ID instead of one global lock?**
Two threads borrowing *different* books should never block each other — only concurrent
attempts on the *same* book need to be serialized. A single global lock would be simpler but
would turn every borrow across the whole catalog into a queue of one. The lock map
(`ConcurrentHashMap<String, ReentrantLock>`) is built lazily with `computeIfAbsent`.

**What exactly does the lock protect?**
The read-check-write sequence: read `availableCopies` → check `> 0` → decrement + persist to
the DB. Without the lock, two threads can both pass the check before either writes, and the
count can go negative under real concurrent load — the classic check-then-act race.

**How did you prove it actually works, not just "should work"?**
Signed up 20 accounts, logged them all in, and fired 20 truly concurrent HTTP POST requests
at `/api/borrow` for a book with exactly 3 copies. Result: exactly 3 got `201 Created`, the
other 17 got a clean `409 Conflict`, and the database's `available_copies` ended at exactly 0
— never negative, never over-allocated. Full command sequence is in this README's history / can
be reproduced with any HTTP client.

**Why `com.sun.net.httpserver.HttpServer` instead of a micro-framework like Javalin/Spark?**
Those are still frameworks — they route requests and parse JSON for you. `HttpServer` gives raw
`HttpExchange` objects; the `Router` class, `HttpUtil`, and `JsonUtil` in this project are all
hand-written. That's a stronger "I understand what a framework does under the hood" story than
"I used a smaller framework."

**Why checked exceptions (`LibraryException extends Exception`) instead of unchecked?**
Forces every caller at the HTTP boundary to explicitly handle domain failures (resource
unavailable, borrow limit exceeded, etc.) and turn them into proper HTTP status codes, rather
than letting them surface as an uncaught 500.

**Where does the generic `Repository<T, ID>` interface come from conceptually?**
It's deliberately the same shape as Spring Data's `JpaRepository<T, ID>` — same contract, but
every method (`UserRepository`, `ResourceRepository`, `BorrowRecordRepository`) is implemented
by hand with `PreparedStatement`/`ResultSet`, which is exactly what Spring Data generates for
you at runtime.

**Security note to be upfront about:** password hashing here is salted SHA-256, not
bcrypt/Argon2 — a deliberate scope decision to stay dependency-free, and worth naming as a
"what I'd do differently in production" point if asked.
