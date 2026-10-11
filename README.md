# Web Tree Parser

## Description

Web Tree Parser is a web application built with Spring Boot. It parses a string that represents a mathematical set, builds a tree of its structure and calculates statistics.

The supported element types are numbers, identifiers, strings and nested sets.

**Features:**
- Tree representation of a set and statistics: total elements, sets, subsets, max depth, numbers, identifiers, strings.
- Registration, sign in and two roles: `USER` and `ADMIN`.
- Personal parse history stored in a database. An administrator sees the history of all users.
- History search by text and filter by date, with pagination.
- Import of a set from a `.txt` file.
- Contains CI in Github Actions.
- Export of a result to JSON or XML.
- Administrator panel: user list, role management, account blocking.

## Future improvements

### What to do in the future?

- **Caching of the current user check.** Keep id, role, enabled flag in a cache and take it on every administrator action (this removes the query on each request).
- **Role hierarchy.** Add a `SUPER_ADMIN` role that manages administrators while `ADMIN` manages only regular users. This would replace the special rules for the last administrator (also maybe it needs a migration of the `role` check constraint).
- **Search.** User search by username in the administrator panel, a history filter by author for administrators, and a `pg_trgm` index for substring search on large histories.
- **Accounts.** Password change, deleting own history entries, protection against password guessing.
- **Operations.** Spring Boot Actuator health endpoint with a container health check, CI/CD.
- **Localization.** Russian and English interface.
- **Actuator**. Restrict for all users (connect actuator to Kubernetes and Prometheus instead which is sufficient for a test assignment).

## Stack

- Java 25
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Testcontainers
- Thymeleaf
- Gradle
- Docker, Docker Compose
- Jakarta Validation
- Lombok
- JUnit
- Mockito
- AssertJ

## Run

Docker and Docker Compose must be installed.

```bash
git clone https://github.com/fezlr/web-tree-parser.git
cd web-tree-parser
cp .env.example .env
docker compose up --build
```

Open `http://localhost:8080` (the port is `APP_PORT=8080` in `.env`).

Change the passwords in `.env` before the first start. The `.env` file is not committed to the repository.

### Environment variables

| Variable | Purpose |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Database |
| `POSTGRES_PORT` | Database port |
| `APP_PORT` | Application port |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Default administrator account |

### Development

Run the database in Docker and the application with Gradle:

```bash
docker compose up -d postgres
./gradlew bootRun
```

Tests start their own PostgreSQL through Testcontainers (Docker must be running):

```bash
docker compose up -d postgres
./gradlew test
```

To remove all data and start from scratch:

```bash
docker compose down -v
```

## Accounts and roles

### Default accounts

On the first start the application creates an administrator with the username and password from `ADMIN_USERNAME` and `ADMIN_PASSWORD` in `.env`.
If the user already exists, nothing is changed (including the password).

User can register on the `/register` page.

Registration rules:
- Username: 3-50 characters (letters, digits, `.`, `_`, `-` and also names are unique ignoring case).
- Password: 8-100 characters.

### Roles

| Capability | USER | ADMIN |
|---|---|---|
| Parse a set (text input or file) | yes | yes |
| View history | own entries | entries of all users |
| Open and export a result | own results | any result |
| Administrator panel `/admin/users` | no | yes |
| Change roles, block accounts | no | yes |

If you a user and try to open result page of another user, you will get 404 error page.

Administrator rules:
- Administrator cannot change their own role or block themselves.
- The last active administrator cannot be demoted or blocked.
- Blocking and role changes take effect immediately on the next request of the user.

## Usage

| Page | Description | Access |
|---|---|---|
| `/login`, `/register` | Sign in and registration | everyone |
| `/` | Parse a set from the text field or from a file | signed in |
| `/results/{id}` | Tree and statistics of a saved parse | owner, administrator |
| `/results/{id}/export?format=json` or `format=xml` | Download a result | owner, administrator |
| `/history` | History with search, date filter and pagination | signed in |
| `/admin/users` | User management | administrator |
| `/actuator/health` | Health check, reports only the status | everyone (open for k8s for instance; need to restrict for everyone in production) |


### Input syntax

A set is written in braces.
Elements are separated by commas, spaces and line breaks are ignored.
An element can be a number (`1`, `2.5`), an identifier (`a`, `x1`), a string in double quotes (`"text"`) or a nested set.

```
{1, {a, "text"}, {}}
```

Parse errors show the position of the unexpected character.

### Limits

- Input: up to ~100,000 characters.
- Import file: `.txt`, UTF-8, up to ~200 KB and 100,000 characters.

### History

Every successful parse is saved with the string, the author, the time and the statistics.
The search is a case-insensitive substring match on the source string.
The date filter uses the server time zone.
A page holds 20 entries.

### Export

The exported file contains metadata, string, tree as a nested structure and the statistics.
XML cannot store some control characters, so they are replaced with `U+FFFD` in the XML file.
JSON keeps the source string exactly.

## Examples

`{a, {b, c}, d}`:
```
Set
├── a
├── Set
│   ├── b
│   └── c
└── d

Total elements: 4
Sets: 2
Subsets: 1
Max depth: 2
Numbers: 0
Identifiers: 4
Strings: 0
```

`{{1, 2}, {3, {4, 5}}, 6}`:
```
Set
├── Set
│   ├── 1
│   └── 2
├── Set
│   ├── 3
│   └── Set
│       ├── 4
│       └── 5
└── 6

Total elements: 6
Sets: 4
Subsets: 3
Max depth: 3
Numbers: 6
Identifiers: 0
Strings: 0
```

`{1, {a, "text"}}`:
```
Set
├── 1
└── Set
    ├── a
    └── text

Total elements: 3
Sets: 2
Subsets: 1
Max depth: 2
Numbers: 1
Identifiers: 1
Strings: 1
```

## Technical approach

Packages:

| Package | Responsibility |
|---|---|
| `core` | Parsing without Spring: `lexer`, `node`, `parser`, `printer`, `stats` |
| `parsing` | Parser page, file import, file export |
| `history` | Saving and viewing history, search and filters |
| `export` | JSON and XML export |
| `user` | User entity, roles, user service |
| `auth` | Registration, sign in, `UserDetails` |
| `admin` | Administrator panel |
| `config` | Security configuration, default administrator |
| `common` | AppConstants |

Inside `core`:
- **`lexer`**: makes tokens from a string. Contains `Lexer`, `Token`, `TokenType`.
- **`node`**: the tree model. `Node` is a marker interface implemented by `SetNode`, `NumberNode`, `IdentifierNode`, `StringNode`.
- **`parser`**: a recursive parser that builds `Node` from tokens. It limits nesting depth.
- **`printer`**: renders a tree as text.
- **`stats`**: collects statistics of a finished tree.

Database: PostgreSQL with two tables, `users` and `parse_history`. The schema is managed by Flyway migrations in `src/main/resources/db/migration`. Hibernate only validates that the entities match the schema.

### Design decisions

- **History stores the string and statistics** the tree is rebuilt when a result is opened or exported. This keeps rows small and does not require a migration if tree format changes.
- **Export belongs to a history** is the same access check is used.
- **Passwords** are hashed with BCrypt. CSRF protection is enabled for all forms.
- **Current user check** a filter reloads the user on every request (except static resources such as css, js and etc.). That is why blocking and role changes apply immediately (but it costs an additional request. For this small application is ok).
- **Registration rules** (password confirmation, unique username) live in the registration service. Controllers only turn results into messages for the page.
- **Input limits** protect from stack overflow on deeply nested input (up to ~ `100.000` characters).
- **XML** is written with the standard StAX API.

### Tests

- Unit-tests
- Integration tests

Test:
```bash
docker compose up -d postgres
./gradlew test
```
