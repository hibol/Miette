# Miette — Recipe Manager

A personal Spring Boot web application for managing and browsing recipes, with full-text search, tag filtering, and a secure admin interface.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5 |
| Templating | Thymeleaf + Spring Security extras |
| Frontend | Bootstrap 5, Bootstrap Icons, Vue 3, SortableJS, Klee One (Google Fonts) |
| Database | MySQL |
| Search | Hibernate Search 7 + Lucene (embedded, ngram analyzer) |
| Security | Spring Security (BCrypt, Remember Me) |
| Storage | Cloudflare R2 (prod), MinIO (local dev) |
| Image processing | Thumbnailator + webp-imageio (WebP conversion) |
| Deployment | Hetzner VPS (Docker Compose + Caddy) |
| CI/CD | GitHub Actions → GHCR → SSH deploy |
| Build | Maven |

---

## Features

**Browsing and search**
- Full-text substring search across recipe titles, ingredients, steps, and tags (Hibernate Search + Lucene ngram)
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

**Photos (admin)**
- Upload photos directly on the recipe page
- Images are automatically converted to WebP and resized on upload: full version at 1920px (92% quality) and thumbnail at 400px (85% quality), both stored in object storage
- EXIF orientation is applied on resize so phone photos always display upright
- Photo date is extracted from EXIF metadata (`DateTimeOriginal`) and falls back to the current date if absent
- Photos are browsable via an infinite carousel (previous/next navigation) with the capture date displayed

**On-demand glossary**
- Dedicated glossary page (`/glossaire`) listing culinary terms alphabetically, with a sticky letter index (sidebar on desktop, scrollable bar on mobile)
- On any recipe page, a "?" button highlights all recognized terms and aliases inline; tooltips show the definition on hover (desktop) or tap (mobile)
- Terms are fetched lazily on first activation and cached for the session
- Admins can add, edit, and delete terms and their aliases directly from the glossary page

**Visual identity**
- A rotating cuboctahedron (wireframe, 12 vertices, 24 edges) serves as the app's loading indicator: full-page before Vue mounts, inline spinner on async buttons
- Animation runs via Canvas 2D with perspective projection and depth-based edge weight; DPR-aware for retina screens

**Recipe characteristics**
- Hydration rate calculated from water, milk, and levain (counted at 100% hydration); displayed with depth indicators; annotated when fat or eggs are present
- "Ajouts" indicator flags any ingredient outside the configurable standard list (farine, eau, sel, levain, levure, lait)
- Gluten strength score computed as a quantity-weighted average of ingredient `gluten_strength` values (0–1); displayed as fort / moyen / faible (thresholds 0.75 / 0.5) when at least one flour ingredient has a value set

**Admin**
- Create, edit, and delete recipes
- Maintenance page: rebuild the search index, set gluten strength values per flour ingredient, view and delete orphan ingredients
- Generic key/value settings table (`app_setting`) editable from the admin page — currently used for the standard ingredient keywords list

---

## Database Schema

| Table | Description |
|---|---|
| `recipe` | Core recipe with title, creation and modification metadata |
| `phase` | Named preparation phase, ordered by position, linked to a recipe |
| `step` | Individual step within a phase, ordered by position |
| `ingredient` | Ingredient with label, optional unit, and optional `gluten_strength` (0–1) |
| `ingredient_rel_phase` | Junction table: quantity of an ingredient within a phase |
| `tag` | Unique label-based tag |
| `recipe_rel_tag` | Many-to-many between recipe and tag |
| `asset` | Media file with date, path, and description |
| `recipe_rel_asset` | Many-to-many between recipe and asset; `cover` flag marks the thumbnail shown in the list |
| `glossary_term` | Culinary term with definition |
| `glossary_alias` | Alternative names for a glossary term |
| `users` | User accounts with BCrypt password and role (ADMIN / USER) |
| `app_setting` | Generic key/value configuration table; editable from the admin page |

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

```bash
# Standard startup
./run.sh

# Sync prod → local (DB + images) then start
./run.sh --sync-prod
```

`run.sh` starts MinIO via Docker Compose, optionally imports the production database and R2 images, then launches the app.

---

## Deployment

The app runs on a Hetzner VPS (CX22) managed with Docker Compose. Caddy handles HTTPS automatically via Let's Encrypt.

```
Hetzner VPS (CX22)
├── app          Spring Boot container
├── db           MySQL 8 container (persistent volume)
└── caddy        Reverse proxy + automatic HTTPS

Cloudflare R2    Object storage for photos and notes (no egress fees)
```

Every push to `main` triggers a GitHub Actions workflow that builds the Docker image, pushes it to GHCR, and deploys via SSH.

### Environment variables (server `~/miette/.env`)

| Variable | Description |
|---|---|
| `DATABASE_HOST` | `db` (Docker Compose service name) |
| `DATABASE_PORT` | `3306` |
| `DATABASE_NAME` | Database name |
| `DATABASE_USER` | Database user |
| `DB_PASSWORD` | Database password (shared by app and MySQL container) |
| `DB_ROOT_PASSWORD` | MySQL root password |
| `MIETTE_ADMIN_PASSWORD` | Password for the `hibol` admin account (only needed on first startup) |
| `STORAGE_ENDPOINT` | S3-compatible endpoint (Cloudflare R2 jurisdiction-specific URL) |
| `STORAGE_BUCKET` | Bucket name |
| `STORAGE_ACCESS_KEY` | R2 API token access key ID |
| `STORAGE_SECRET_KEY` | R2 API token secret |
| `STORAGE_PUBLIC_URL` | Public base URL for assets |

---

## REST API

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
| `GET` | `/api/glossary` | Public | List all glossary terms with their aliases |
| `POST` | `/api/glossary` | Admin | Create a glossary term |
| `PUT` | `/api/glossary/{id}` | Admin | Update a glossary term and its aliases |
| `DELETE` | `/api/glossary/{id}` | Admin | Delete a glossary term and all its aliases |

All write endpoints require `ROLE_ADMIN` and CSRF token. Validation errors are returned as `{"errors": {"field": "message"}}`.

---

## Security

- Spring Security with BCrypt password hashing
- Role-based access: `ROLE_ADMIN` required for all write operations and `/admin/**` routes
- Remember Me token valid for 24 hours
- CSRF protection enabled (token passed via meta tags for AJAX calls)
