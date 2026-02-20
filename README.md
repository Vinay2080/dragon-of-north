# Dragon of North

A production-style **authentication and session management platform** built with **Spring Boot + React**.

This project is designed for internship/job interviews and demonstrates:

- secure authentication architecture,
- OTP lifecycle design,
- device-aware session management,
- distributed rate limiting,
- structured API error contracts,
- full-stack integration.

---

## Table of Contents

- [Overview](#overview)
- [Why This Project Stands Out](#why-this-project-stands-out)
- [Core Features](#core-features)
- [Architecture](#architecture)
- [Visual Flows](#visual-flows)
- [Security Design Decisions](#security-design-decisions)
- [API Overview](#api-overview)
- [API Examples](#api-examples)
- [Data Model](#data-model)
- [Error Handling Contract](#error-handling-contract)
- [Rate Limiting](#rate-limiting)
- [Frontend Highlights](#frontend-highlights)
- [Testing](#testing)
- [Local Setup](#local-setup)
- [Deployment Notes](#deployment-notes)
- [Interview Talking Points](#interview-talking-points)
- [Roadmap](#roadmap)
- [Project Structure](#project-structure)
- [License](#license)

---

## Overview

Dragon of North implements a complete auth journey:

1. Identifier status check
2. Signup initiation
3. OTP request + verification
4. Signup completion
5. Login with access/refresh cookies
6. Device-aware session creation
7. Refresh token rotation
8. Logout and session revoke flows

This is intentionally **not** a simple login/signup demo — it includes real-world auth lifecycle concerns.

---

## Why This Project Stands Out

- End-to-end auth and session lifecycle
- JWT access/refresh split with explicit token-type handling
- HttpOnly cookie-first model + Bearer compatibility
- Refresh token hash-at-rest session persistence
- Purpose-aware OTP engine (signup/login/reset/2FA)
- Redis-backed distributed throttling with Bucket4j
- Global exception handling + enum-based error contract
- Frontend integrated with refresh-on-401 retry flow
- Session dashboard with revoke controls

---

## Core Features

### 1) Authentication & User Lifecycle

- Identifier-based auth: **EMAIL** and **PHONE**
- Status model:
    - `NOT_EXIST`
    - `CREATED`
    - `VERIFIED`
    - `DELETED`
- Signup is split into:
    - `/sign-up` (create account in `CREATED`)
    - `/sign-up/complete` (promote to `VERIFIED`)

### 2) JWT + Cookie Model

- Access token for protected API auth
- Refresh token for refresh-only endpoint
- `token_type` claim validation (`access_token`, `refresh_token`)
- RSA signing and verification
- Access token extraction supports:
    - `Authorization: Bearer <token>`
    - `access_token` cookie

### 3) Device-Aware Session Management

- Session stores:
    - `device_id`
    - `ip_address`
    - `user_agent`
    - `last_used_at`
    - `expiry_date`
    - `revoked`
- Session operations:
    - list all sessions
    - revoke one session by ID
    - revoke all other sessions (except current device)
- Refresh flow rotates refresh token hash

### 4) OTP Engine

- Email OTP + phone OTP
- Purpose-scoped OTP:
    - `SIGNUP`
    - `LOGIN`
    - `PASSWORD_RESET`
    - `TWO_FACTOR_AUTH`
- Verification outcomes:
    - `SUCCESS`
    - `INVALID_OTP`
    - `EXPIRED_OTP`
    - `MAX_ATTEMPT_EXCEEDED`
    - `ALREADY_USED`
    - `INVALID_PURPOSE`
- OTP stored hashed (BCrypt)

### 5) Abuse Prevention

- Endpoint-specific rate limits
- Redis distributed token bucket state (Bucket4j)
- Client-visible headers:
    - `X-RateLimit-Remaining`
    - `X-RateLimit-Capacity`
    - `Retry-After`

### 6) Operations Readiness

- PostgreSQL persistence
- Redis integration
- AWS SES/SNS OTP delivery hooks
- Prometheus + Actuator
- Scheduled cleanup for OTP/session/unverified-user records
- Docker-ready backend

---

## Architecture

```text
React Frontend
   ↓
Spring Boot REST API
   ├── Controllers (Auth / OTP / Sessions)
   ├── Services (Business logic)
   ├── Repositories (JPA)
   ├── Security (JWT filter + auth manager)
   ├── Rate Limiter (Redis + Bucket4j filter)
   ├── Exception Layer (global mapping)
   ├── Scheduler (cleanup tasks)
   └── Integrations (AWS SES/SNS)
        ↓
PostgreSQL + Redis
