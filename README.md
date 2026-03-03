# Secure Distributed Transaction Platform

End-to-end distributed transaction validation platform built from scratch using:

- **Java (Spring Boot)** for coordinator/orchestrator APIs
- **C++ (OpenSSL + threads)** for validator node services
- **PostgreSQL** for durable transaction storage
- **Linux shell automation** for SSL bootstrap and stress testing

## High-Level Architecture

1. Client submits a transaction to the Java coordinator (`POST /api/transactions`).
2. Coordinator persists initial transaction state in PostgreSQL.
3. Coordinator establishes **mTLS (SSL/OpenSSL)** connections to C++ validator nodes.
4. Validators perform deterministic checks and return decisions (`APPROVE` or `REJECT`).
5. Coordinator applies quorum policy and updates transaction status.

## Security Enhancements

- TLS 1.2+ for coordinator-validator communication
- Mutual TLS (validators require client cert)
- CA-based trust chain for all nodes
- Hostname verification enabled in Java TLS client

## Features

- Concurrent request handling on Java and C++ sides
- Quorum validation workflow (majority voting)
- PostgreSQL persistence and status history
- Request validation and idempotency by `reference`
- Load/stress scripts for reliability checks
- Docker Compose for local end-to-end setup

## Quick Start

### 1) Generate local certificates

```bash
./scripts/generate-certs.sh
```

### 2) Build and run with Docker Compose

```bash
docker compose up --build
```

### 3) Submit transaction

```bash
curl -k -X POST https://localhost:8443/api/transactions \
  -H 'Content-Type: application/json' \
  -d '{"sender":"alice","receiver":"bob","amount":125.5,"currency":"USD","reference":"invoice-991"}'
```

### 4) Fetch transaction status

```bash
curl -k https://localhost:8443/api/transactions/invoice-991
```

### 5) Stress test

```bash
./scripts/stress-test.sh 200 20
```
