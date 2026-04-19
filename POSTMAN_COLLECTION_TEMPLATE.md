# Postman Collection Template

**Purpose:** Whenever an agent generates a REST API in code, it also generates a matching Postman v2.1 collection. You can then import into Postman (manual testing) or run via Newman CLI (CI-style). Complements QA agent's Testcontainers tests — those validate internal logic, Postman validates external HTTP contracts.

**When to use:** Automatically generated alongside any Spring Boot / REST project.

**Output location:** `workspace-{agent}/code/{YYYY-MM-DD}-{topic-slug}-demo/postman/`

---

## Files to Generate

```
postman/
├── {topic-slug}.postman_collection.json       ← The collection (v2.1 schema)
├── {topic-slug}.postman_environment.json      ← Environment variables (baseURL, tokens)
├── newman-run.sh                               ← CLI runner
└── README.md                                   ← Import instructions
```

---

## Collection Structure (JSON v2.1)

Every collection follows this shape. Agents generate it dynamically by scanning `@RestController` classes in the project.

```json
{
  "info": {
    "name": "{Topic Title}",
    "description": "API collection for {topic-slug}. See design doc: ../../docs/{date}-{topic-slug}.md",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    "_postman_id": "{UUID}"
  },
  "auth": {
    "type": "bearer",
    "bearer": [{ "key": "token", "value": "{{authToken}}", "type": "string" }]
  },
  "variable": [
    { "key": "baseUrl", "value": "{{baseUrl}}" }
  ],
  "item": [
    {
      "name": "{ControllerName}",
      "description": "Endpoints grouped by controller",
      "item": [
        {
          "name": "{EndpointName}",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"field1\": \"value1\",\n  \"field2\": 123\n}"
            },
            "url": {
              "raw": "{{baseUrl}}/api/v1/{resource}",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v1", "{resource}"]
            },
            "description": "{Endpoint purpose in plain English}"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code is 201', () => pm.response.to.have.status(201));",
                  "pm.test('Response has id field', () => {",
                  "  const json = pm.response.json();",
                  "  pm.expect(json).to.have.property('id');",
                  "  pm.collectionVariables.set('lastCreatedId', json.id);",
                  "});",
                  "pm.test('Response time < 500ms', () => pm.expect(pm.response.responseTime).to.be.below(500));"
                ]
              }
            }
          ]
        }
      ]
    }
  ]
}
```

---

## Environment File Structure

Stores base URL + tokens. Never commit a production environment with real secrets.

```json
{
  "id": "{UUID}",
  "name": "{topic-slug} local",
  "values": [
    { "key": "baseUrl", "value": "http://localhost:8080", "enabled": true },
    { "key": "authToken", "value": "dev-token", "enabled": true, "type": "secret" },
    { "key": "lastCreatedId", "value": "", "enabled": true }
  ],
  "_postman_variable_scope": "environment"
}
```

---

## newman-run.sh (CLI Runner)

```bash
#!/usr/bin/env bash
# Run the Postman collection from CLI — usable in CI
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COLLECTION="$SCRIPT_DIR/{topic-slug}.postman_collection.json"
ENVIRONMENT="$SCRIPT_DIR/{topic-slug}.postman_environment.json"

if ! command -v newman &> /dev/null; then
  echo "Newman not installed. Run: npm install -g newman"
  exit 1
fi

echo "Running Postman collection against $(jq -r '.values[0].value' "$ENVIRONMENT")"

newman run "$COLLECTION" \
  --environment "$ENVIRONMENT" \
  --reporters cli,junit,html \
  --reporter-junit-export "$SCRIPT_DIR/newman-junit.xml" \
  --reporter-html-export "$SCRIPT_DIR/newman-report.html" \
  --bail

echo "Done. Reports in $SCRIPT_DIR/"
```

---

## README.md (in postman/ folder)

```markdown
# Postman Collection — {Topic Title}

## Quick import

1. Open Postman desktop app
2. Click **Import** (top left) → drop both JSON files:
   - `{topic-slug}.postman_collection.json`
   - `{topic-slug}.postman_environment.json`
3. In top-right environment dropdown, select `{topic-slug} local`
4. Make sure the backend is running: `docker-compose up` from parent folder
5. Run requests — they'll hit http://localhost:8080 by default

## Run via CLI (CI-style)

\```bash
./newman-run.sh
\```

Requires `npm install -g newman`.

## What's tested
- Response status codes (happy path + error cases)
- Response shape / required fields
- Response time SLA (<500ms)
- Chained workflows (create → get → update → delete)

## Adding new assertions
See example in the "Tests" tab of any request. Uses Chai-style assertions.
```

---

## Required Test Assertions Per Request

Every generated request MUST include these assertions in its `event.test` script:

1. **Status code**: `pm.response.to.have.status({expected});`
2. **Response time SLA**: `pm.expect(pm.response.responseTime).to.be.below(500);`
3. **Content-Type**: `pm.response.to.have.header('Content-Type', /application\/json/);`
4. **Schema validation** (for response bodies): check required fields exist
5. **Variable extraction** (when useful): save IDs for chained requests

---

## Notes for Agents

1. **Generate from code, not from imagination** — scan `@RestController`, `@GetMapping`, `@PostMapping` etc. in the Spring Boot project. Use the DTO classes to generate sample request bodies.
2. **Path variables** use Postman syntax: `/api/v1/orders/:orderId` becomes `{{baseUrl}}/api/v1/orders/:orderId` with `orderId` in the `variable` array.
3. **Auth** — default to `bearer` with `{{authToken}}` env var. If the project uses API keys, adjust.
4. **Chaining** — for workflows like create → get → update, use `pm.collectionVariables.set()` in the first request's tests to save the ID for subsequent requests.
5. **Never commit real tokens** — environment file should have placeholder values only.
