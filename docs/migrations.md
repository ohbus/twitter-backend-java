# Database Migrations

Schema is managed by [Liquibase](https://www.liquibase.org/) via
[`src/main/resources/db/changelog/db.changelog-master.xml`](../src/main/resources/db/changelog/db.changelog-master.xml).
Hibernate runs with `ddl-auto=validate` — it never creates or modifies tables.
**Only Liquibase changes the schema.**

Liquibase applies pending changesets automatically when the application starts.
No manual migration step is required for local development or production — the
application applies migrations before Hibernate's `ddl-auto=validate` check runs.

## Local development

Set up the environment file and start the database:

```bash
# 1. Copy the example environment file
cp .env.example .env

# 2. Set nonempty values in .env for all required Compose variables
#    (see docker-compose.base.yml for the full list).
#    Wrap passwords in single quotes to prevent Compose from interpolating
#    special characters (like $). For an apostrophe in a single-quoted 
#    value, escape it with a backslash (e.g., DB_ROOT_PASS='can\'t').
#    DB_NAME=twitter_clone
#    DB_USER=moo_user
#    DB_PASS='moo_pass'
#    DB_ROOT_PASS='root_pass'
#    JWT_SECRET='<at least 32 bytes of randomness>'

# 3. Start the database in detached mode so the command returns to the shell
docker compose up -d db
```

Maven does not read `.env` — export every value the application needs.
The dev profile reads the database URL from `${DB_NAME:twitter_clone}` and the
root password from `${DB_ROOT_PASS:root_pass}`, and it requires `JWT_SECRET`
to be set (root `application.properties` uses `${JWT_SECRET}` with no default):

```bash
# bash / zsh — use the values from your .env file
# Use single quotes to preserve special characters (like $) in the password.
# If your password contains a single quote, close the quote, insert an escaped 
# quote, and open a new quote (e.g., 'can'\''t').
export DB_NAME='<value of DB_NAME from .env>'
export DB_ROOT_PASS='<value of DB_ROOT_PASS from .env>'
export JWT_SECRET="$(openssl rand -hex 32)"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

```powershell
# PowerShell (Windows) — use the values from your .env file
# Use single quotes to preserve special characters (like $) in the password.
# If your password contains a single quote, double it (e.g., 'can''t').
$env:DB_NAME = '<value of DB_NAME from .env>'
$env:DB_ROOT_PASS = '<value of DB_ROOT_PASS from .env>'
$env:JWT_SECRET = '<at least 32 bytes of randomness>'
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

## Production

Production applies pending changesets on application startup
(`spring.liquibase.enabled=true` in `application.properties`). What happens
when a migration fails depends on the deployment topology:

- **Password Quoting:** Docker Compose `.env` files and shell exports handle special characters differently.
  Always single-quote values containing `$` in `.env` (e.g., `DB_ROOT_PASS='pa$word'`).
  When exporting the same value in a shell, use the shell-specific single-quote escaping shown above.
  If you use double quotes in a shell, the shell will expand `$`.
- **Rolling / blue-green deployments** (two app instances, traffic routed
  between them): the previous instance keeps serving traffic while the new
  instance applies migrations. If a migration fails, only the new instance is
  affected, and the old one continues serving.
- **Single-container Compose** (this repository's default deployment):
  `docker compose up` stops and recreates the app container when its image
  changes. A migration failure at startup leaves no version serving traffic
  until the failure is resolved and a working image is redeployed.

For zero-downtime migrations, use a multi-instance setup where the previous
version keeps running while the new version applies changes.

Rollback is **not a supported recovery path**. The current changelog does
not define `<rollback>` blocks, so `liquibase:rollback` can only reverse
changesets via Liquibase's auto-generated inverse operations — which for
`010-create-refresh-tokens-table` means dropping the table and all stored
refresh tokens. If a migration needs to be reversed, deploy a new
forward-only changeset that compensates for the previous one.