# Documentation tooling

This directory contains build dependencies for the documentation site. It is
excluded from published content so tooling files do not appear in navigation or
search.

## Contents

| File | Purpose |
| --- | --- |
| `requirements.in` | Declares the direct Python dependency |
| `requirements.txt` | Locks the resolved Python dependency graph and hashes |
| `package.json` | Defines local Redocly commands and the pinned CLI dependency |
| `package-lock.json` | Reproduces the Node dependency graph in CI |
| `redocly.yaml` | Validates and configures the generated OpenAPI reference |

## Build locally

From the repository root:

```bash
python3.13 -m venv docs/tooling/.venv
docs/tooling/.venv/bin/pip install --require-hashes --only-binary :all: -r docs/tooling/requirements.txt
npm --prefix docs/tooling ci --ignore-scripts
./mvnw -B -Dtest=OpenApiDocumentationTest test
docs/tooling/.venv/bin/zensical build --clean --strict
npm --prefix docs/tooling run api:lint
npm --prefix docs/tooling run api:build
cp target/generated-docs/openapi.json site/api/openapi.json
```

The generated `site/` and `target/` directories are ignored. Do not commit
generated HTML or OpenAPI output.
