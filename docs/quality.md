# Quality and coverage strategy

The target is 100% line and branch coverage for code that makes a product or
security decision. Coverage is evidence of exercised behavior, not the goal by
itself.

## Test priorities

1. Service tests prove state transitions, locking order, idempotency, ownership,
   validation, and repository interactions.
2. Controller tests prove status codes, delegation, authenticated-principal
   behavior, and rejected input.
3. Security tests prove JWT validation, filter behavior, authentication rules,
   and CSRF protection through the MVC boundary.
4. Error-handler tests prove stable public responses and that internal details
   are not returned.
5. The application-context tests prove that JPA mappings and endpoint wiring
   still start against the test database.

Generated mapper implementations, data-only entity/model accessors, Mockito
artifacts, and framework bootstrap/configuration glue are excluded identically
from JaCoCo and Sonar. Any new exclusion requires a reason in the review.

## Verification

```text
./mvnw clean verify
./mvnw jacoco:report
```

When a branch is not covered, first decide whether the behavior is missing a
test or the code is unnecessarily difficult to exercise. Prefer simplifying
the code or adding a focused behavior test. Do not add assertions that only
execute dead code, duplicate another test, or preserve an accidental
implementation detail.
