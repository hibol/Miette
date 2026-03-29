# Miette — Recipe Manager

A personal Spring Boot web application for managing and browsing recipes, with full-text search, tag filtering, and a secure admin interface.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5 |
| Templating | Thymeleaf + Spring Security extras |
| Frontend | Bootstrap 5, Bootstrap Icons, Vue 3, SortableJS |
| Database | MySQL |
| Security | Spring Security (BCrypt, Remember Me) |
| Storage | Cloudflare R2 (prod), MinIO (local dev) |
| Image processing | Thumbnailator + webp-imageio (WebP conversion) |
| Deployment | Railway (Docker) |
| Build | Maven |

---

## Features

**Browsing and search**
- Full-text search across recipe titles, ingredients, steps, and tags (MySQL full-text index)
- Recipe list with tag badges; search results show match count with a one-click reset
- Filter recipes that have notes or photos attached
- Recipe cards show a thumbnail of the latest photo when available

**Recipe display**
- Recipes with a single phase display ingredients and steps directly, without a phase header
- Multi-phase recipes (e.g. dough / filling / glaze) display each phase separately with its own ingredients and steps
- Collapsible notes section below the title, visible to all — shows observation date and content; note count displayed on recipe cards in the list

**Recipe editing (admin)**
- Inline editor on the recipe page — no separate edit page
- Add, rename, reorder, and remove phases, ingredients, and steps
- Drag & drop reordering for phases, ingredients, and steps, including on unsaved recipes
- Tag input with autocomplete on existing tags
- Unsaved-changes warning if navigating away mid-edit
- Inline validation: missing title, missing ingredient name or quantity, unnamed phase in a multi-phase recipe

**Notes (admin)**
- Add timestamped notes to any saved recipe — useful for logging observations, variations, or cooking results
- Date is pre-filled to the current date and time, editable before saving
- Notes are read-only while editing the recipe, to keep both workflows separate

**Photos (admin)**
- Upload photos directly on the recipe page
- Images are automatically converted to WebP and resized on upload: full version at 1920px (92% quality) and thumbnail at 400px (85% quality), both stored in object storage
- EXIF orientation is applied on resize so phone photos always display upright
- Photo date is extracted from EXIF metadata (`DateTimeOriginal`) and falls back to the current date if absent
- Photos are browsable via an infinite carousel (previous/next navigation) with the capture date displayed
- Thumbnails are derived from the full image filename (`*_thumb.ext`) — no extra database column required

**Admin**
- Create, edit, and delete recipes
- Maintenance page: rebuild the full-text search index, view and delete orphan ingredients (ingredients no longer used in any recipe)

**Authentication**
- Login via modal — no redirect away from the current page
- Logout returns to the current page
- Remember Me (24 hours)

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

# Optional: object storage (MinIO via Docker for local dev)
export STORAGE_ENDPOINT=http://localhost:9000
export STORAGE_BUCKET=miette
export STORAGE_ACCESS_KEY=minioadmin
export STORAGE_SECRET_KEY=minioadmin
export STORAGE_PUBLIC_URL=http://localhost:9000/miette
```

Source it before running:

```bash
# Standard startup
./run.sh

# Sync prod → local (DB + images) then start
./run.sh --sync-prod
```

`run.sh` starts MinIO via Docker Compose, optionally imports the Railway database and R2 images, then launches the app. The DB sync patches MySQL → MariaDB incompatibilities on the fly (collation `utf8mb4_0900_ai_ci`, `ngram` parser).

### Local object storage (MinIO)

A `docker-compose.yml` at the project root starts a MinIO instance with the bucket pre-created and set to public read:

```bash
docker-compose up -d
```

MinIO console is available at `http://localhost:9001` (credentials: `minioadmin` / `minioadmin`).

Photo upload works locally without any extra configuration once these variables are set. If `STORAGE_ENDPOINT` is not set, the storage bean is not created and photo upload returns `503`; notes and all other features continue to work normally.

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
| `STORAGE_ENDPOINT` | S3-compatible endpoint (e.g. Cloudflare R2 jurisdiction-specific URL) |
| `STORAGE_BUCKET` | Bucket name |
| `STORAGE_ACCESS_KEY` | R2 API token access key ID |
| `STORAGE_SECRET_KEY` | R2 API token secret |
| `STORAGE_PUBLIC_URL` | Public base URL for assets (e.g. custom domain `https://assets.chez-miette.xyz`) |

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

## Recipe editor

The recipe creation and editing UI is a Vue 3 single-page app served within the Thymeleaf template, split into ES modules (`recipe.js`, `useNotes.js`, `useValidation.js`). It handles both creation and edit modes on the same page (`/recette/new` and `/recette/{id}`).

- Phases, ingredients, and steps are managed in-memory before a single save call
- Phases, ingredients, and steps can be reordered by drag & drop at any time, including before the recipe has been saved for the first time
- A recipe can have a single phase (displayed without a phase title) or multiple named phases
- A phase without ingredients is valid (useful for variant phases that share ingredients with the main phase)
- Blank ingredient and step labels are silently dropped on save; a missing title blocks the save with an explicit error

---

## REST API

The recipe data is exposed through a JSON REST API at `/api/recipes`, consumed by the Vue editor and available for external use.

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/recipes` | Public | List all recipes (supports `?q=` full-text search) |
| `GET` | `/api/recipes/{id}` | Public | Get a single recipe (includes assets) |
| `POST` | `/api/recipes` | Admin | Create a recipe |
| `PUT` | `/api/recipes/{id}` | Admin | Update a recipe |
| `DELETE` | `/api/recipes/{id}` | Admin | Delete a recipe |
| `GET` | `/api/tags` | Public | List all existing tags |
| `POST` | `/api/recipes/{id}/assets` | Admin | Add a note to a recipe |
| `POST` | `/api/recipes/{id}/assets/upload` | Admin | Upload a photo (multipart/form-data, field `file`) |
| `DELETE` | `/api/recipes/{id}/assets/{assetId}` | Admin | Delete an asset (note or photo) |

Requests are validated with Bean Validation (`@NotBlank` on title, `@Positive` on ingredient quantities). Validation errors are returned as structured JSON (`{"errors": {"field": "message"}}`). All write endpoints require `ROLE_ADMIN` and CSRF token.

---

## Testing

Unit tests cover `RecipeWriteService` and `IngredientService` using JUnit 5 + Mockito (no database required).

```bash
./mvnw test
```

Key scenarios covered:
- Two phases saved correctly, including a phase with no ingredients
- Blank ingredient and step labels are filtered before persistence
- `IngredientService.findOrCreate`: creation, reuse without save, unit update

---

## Security

- Authentication via Spring Security with BCrypt password hashing
- Role-based access: `ROLE_ADMIN` required for all write operations and `/admin/**` routes
- Remember Me token valid for 24 hours
- CSRF protection enabled (token passed via meta tags for AJAX calls)
- Login handled via modal (no page navigation), logout returns user to current page
