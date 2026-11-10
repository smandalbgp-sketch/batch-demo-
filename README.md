# Spring Batch + JPA/Hibernate batch upsert REST API

Upload a CSV to a REST endpoint. A Spring Batch job reads it in chunks of 50 and
upserts into the `customers` table (insert new emails, update existing ones) using
Hibernate JDBC batching.

## Run

```bash
mvn spring-boot:run          # Java 17+, uses in-memory H2
```

## Try it

```bash
# 1. Start the import (returns 202 + jobExecutionId immediately)
curl -F "file=@sample.csv" http://localhost:8080/api/customers/import

# 2. Poll progress
curl http://localhost:8080/api/customers/import/1

# 3. See the data
curl http://localhost:8080/api/customers
curl http://localhost:8080/api/customers/count
```

Upload the same file again with a changed city: rows are updated, not duplicated.
The row with `not-an-email` is filtered (see `filterCount`).

## How batching works here

| Piece | Where | Why |
|---|---|---|
| `hibernate.jdbc.batch_size=50` | application.properties | groups INSERT/UPDATE statements into JDBC batches |
| `order_inserts` / `order_updates` | application.properties | keeps statements for the same table together so batches are not broken up |
| `GenerationType.SEQUENCE`, `allocationSize=50` | `Customer` | IDENTITY disables insert batching |
| `chunk(50, txManager)` | `BatchConfig` | one transaction and one flush per 50 items, matching batch_size |
| `findByEmailIn` per chunk | `CustomerUpsertWriter` | one SELECT per chunk to decide insert vs update (no N+1) |
| `generate_statistics=true` | application.properties | logs "executed N JDBC batches" per session so you can confirm batching |

## Switching to MySQL / PostgreSQL

Replace the H2 dependency and datasource URL. For MySQL add
`?rewriteBatchedStatements=true` to the JDBC URL.
