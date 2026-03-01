# Miette — Recipe Manager

A personal Spring Boot web application for managing and browsing recipes, with full-text search, tag filtering, and a secure admin interface.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5 |
| Templating | Thymeleaf + Spring Security extras |
| Frontend | Bootstrap 5, Bootstrap Icons |
| Database | MySQL |
| Security | Spring Security (BCrypt, Remember Me) |
| Deployment | Railway (Docker) |
| Build | Maven |

---

## Features

- Browse and search recipes by title, ingredients, steps, and tags (MySQL full-text search)
- Recipes support multiple named phases (e.g. dough / filling / assembly), each with their own ingredients and steps
- Tag system for filtering recipes
- Asset management (images linked to recipes)
- Secure admin interface for creating, editing, and deleting recipes
- Login modal (no redirect away from current page)
- Smart logout returning to the current page

---

## Database Schema

| Table | Description |
|---|---|
| `recipe` | Core recipe with title |
| `phase` | Named preparation phase, ordered by position, linked to a recipe |
| `step` | Individual step within a phase, ordered by position |
| `ingredient` | Ingredient with label and optional unit |
| `ingredient_rel_phase` | Junction table: quantity of an ingredient within a phase |
| `tag` | Unique label-based tag |
| `recipe_rel_tag` | Many-to-many between recipe and tag |
| `asset` | Media file with date, path, and description |
| `recipe_rel_asset` | Many-to-many between recipe and asset |
| `recipe_search_index` | Full-text search index aggregating recipe content |
| `users` | User accounts with BCrypt password and role (ADMIN / USER) |

---

## Local Setup

### Prerequisites

- Java 17+
- Maven (or use the included `./mvnw` wrapper)
- MySQL instance (local or remote)

### Environment variables

Create a `.env.local` file (not committed):

```dotenv
export DATABASE_HOST=localhost
export DATABASE_PORT=3306
export DATABASE_NAME=miette
export DATABASE_USER=your_user
export DATABASE_PASSWORD=your_password
```

Source it before running:

```bash
source .env.local
./mvnw spring-boot:run
```

---

## Deployment (Railway)

The app and database are both hosted on [Railway](https://railway.app). The app is containerized via a `Dockerfile` at the project root using `eclipse-temurin:17-jdk-alpine`.

### Required environment variables in Railway

| Variable | Description |
|---|---|
| `DATABASE_HOST` | MySQL host provided by Railway |
| `DATABASE_PORT` | MySQL port (default: 3306) |
| `DATABASE_NAME` | Database name |
| `DATABASE_USER` | Database user |
| `DATABASE_PASSWORD` | Database password |
| `MIETTE_ADMIN_PASSWORD` | Password for the `hibol` admin account (only needed on first startup) |

### Build & Start

Handled automatically by the Dockerfile:

```dockerfile
RUN ./mvnw clean package -DskipTests
CMD ["./mvnw", "spring-boot:run"]
```

---

## Data Seeding

On first startup, if the database is empty, recipes are automatically seeded from `src/main/resources/recipes.yaml`. The YAML format supports both simple recipes (flat ingredients/steps) and multi-phase recipes.

The admin user is also created on first startup if the `MIETTE_ADMIN_PASSWORD` environment variable is set. If not set, the user is skipped and can be created later by setting the variable and restarting.

| Variable | Description |
|---|---|
| `MIETTE_ADMIN_PASSWORD` | Password for the `hibol` admin account |

The full-text search index (`recipe_search_index`) aggregatestitle, tags, ingredients, and steps into a single searchable column.

---

## Security

- Authentication via Spring Security with BCrypt password hashing
- Role-based access: `ROLE_ADMIN` required for all `/admin/**` routes
- Remember Me token valid for 24 hours
- CSRF protection enabled (token passed via meta tags for AJAX login)
- Login handled via modal (no page navigation), logout returns user to current page
