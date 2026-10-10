# Auth Backend — Spring Boot JWT + OAuth2 Authentication Service

![Java](https://img.shields.io/badge/Java-21-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.10-brightgreen)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-6DB33F)
![JWT](https://img.shields.io/badge/JWT-jjwt%200.13-black)
![OAuth2](https://img.shields.io/badge/OAuth2-Google-4285F4)
![MariaDB](https://img.shields.io/badge/MariaDB-Database-003545)
![Swagger](https://img.shields.io/badge/OpenAPI-Swagger%20UI-85EA2D)

A complete, production-ready authentication backend built with Spring Boot 3. Supports **email/password login**, **Google OAuth2 social login**, **JWT access tokens**, **rotating refresh tokens** (stored in the database and delivered via HTTP-only cookies), **RBAC**, and full **OpenAPI documentation**.

---

## Table of Contents

1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Features](#features)
5. [Project Structure](#project-structure)
6. [Data Model](#data-model)
7. [Security Design](#security-design)
8. [API Reference](#api-reference)
9. [Getting Started](#getting-started)
10. [Configuration](#configuration)
11. [Environment Profiles](#environment-profiles)
12. [Token Flow Diagrams](#token-flow-diagrams)

---

## Overview

This service provides a standalone authentication layer that any frontend or mobile client can consume. It is intentionally decoupled — no business domain logic lives here. The service issues **short-lived JWT access tokens** (default: 10 min) and **long-lived refresh tokens** (default: 24 hrs) with full rotation and revocation support.

Two login paths are available out of the box:

- **Local** — email + password, with BCrypt hashing
- **Social** — Google OAuth2; user is auto-provisioned on first login, then recognized by email on subsequent logins

---

## Tech Stack

| Concern | Technology |
|---------|-----------|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3.5.10 |
| Security | Spring Security 6 · OAuth2 Client |
| JWT | jjwt 0.13.0 (HS512) |
| Persistence | Spring Data JPA · Hibernate |
| Database | MariaDB / MySQL |
| DTO Mapping | ModelMapper 3.2.4 |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI 2.8.17 (Swagger UI) |
| Build | Maven (wrapper included) |

---

## Architecture

```
┌────────────────────────────────────────────┐
│       Client (Browser / Mobile App)        │
│                                            │
│  POST /api/v1/auth/login                   │
│  POST /api/v1/auth/register                │
│  POST /api/v1/auth/refresh   (cookie/body) │
│  POST /api/v1/auth/logout                  │
│  GET  /oauth2/authorization/google         │
└──────────────────┬─────────────────────────┘
                   │ HTTP
                   ▼
┌────────────────────────────────────────────┐
│         Spring Boot Auth Service           │
│                                            │
│  AuthController  UserController            │
│  JwtService      JwtAuthenticationFilter   │
│  Oauth2SuccessHandler  CookieService       │
│  SecurityConfig  CustomUserDetailService   │
└────────┬───────────────────────────────────┘
         │ Spring Data JPA
         ▼
┌─────────────────────┐    ┌────────────────────────┐
│  MariaDB / MySQL    │    │  Google OAuth2 Server   │
│  users              │    │  (Spring Security       │
│  roles              │    │   OAuth2 Client)        │
│  refresh_tokens     │    └────────────────────────┘
└─────────────────────┘
```

---

## Features

### Authentication

- **Register** — creates a `User` with `GUEST` role, BCrypt-hashed password
- **Login** — validates credentials via `AuthenticationManager`, issues access + refresh tokens
- **Refresh** — accepts token from HTTP-only cookie, request body, `X-Refresh-Token` header, or `Authorization: Bearer` header (in that priority order); rotates the refresh token (old JTI is revoked, new one issued); replaces the cookie
- **Logout** — revokes the current refresh token in the database, clears the cookie, clears the `SecurityContextHolder`

### Google OAuth2 Social Login

- Redirect to `/oauth2/authorization/google` to start the flow
- `Oauth2SuccessHandler` fires after Google authentication succeeds:
  - Extracts `sub` (Google ID), `email`, `name`, `picture` from the OAuth2 principal
  - Auto-creates a `User` row if none exists for that email; otherwise reuses the existing one
  - Sets `Provider.GOOGLE` on the user entity
  - Issues the same access + refresh token pair as the local login path
  - Attaches the refresh token as an HTTP-only cookie
- GitHub support is scaffolded in comments — enable by adding the client registration and uncommenting the `"github"` case

### Token Security

- **Access token** — short-lived (600s default), carries `sub` (user UUID), `email`, `roles`, `typ: "access"`, signed with HS512
- **Refresh token** — long-lived (86400s default), carries `jti` (UUID), `sub`, `typ: "refresh"`, signed with HS512; stored in `refresh_tokens` table
- **Rotation** — every `POST /refresh` revokes the old JTI (`revoked=true`, `replacedByToken` recorded) and issues a new one. A revoked token is rejected immediately.
- **HTTP-only cookie** — refresh token is delivered as a `Secure; HttpOnly; SameSite=Lax` cookie (all attributes configurable); never readable by JavaScript
- **Cookie fallback** — if no cookie is present, the endpoint also accepts the refresh token in the request body or the `X-Refresh-Token` header, making it usable from native mobile clients

### Role-Based Access Control

| Role | Assigned to | Access |
|------|-------------|--------|
| `GUEST` | All registered / social users | Standard user endpoints |
| `ADMIN` | Manually promoted users | Full `UserController` CRUD |

Both roles are seeded automatically at application start via `AppConstants`.

### User Management (Admin only)

- `GET /api/v1/users/` — list all users
- `POST /api/v1/users/` — create user
- `GET /api/v1/users/{userId}` — get by ID
- `GET /api/v1/users/email/{email}` — get by email
- `POST /api/v1/users/{userId}` — update user
- `DELETE /api/v1/users/{userId}` — delete user

### API Documentation

Full interactive Swagger UI at `http://localhost:8083/swagger-ui.html` (dev port). Configured via `APIDocConfig.java` (springdoc-openapi).

---

## Project Structure

```
src/main/java/com/example/auth_app_backend/
│
├── AuthAppBackendApplication.java
│
├── config/
│   ├── APIDocConfig.java          # SpringDoc / Swagger UI configuration
│   ├── AppConstants.java          # Role names, seeds ADMIN + GUEST on startup
│   ├── ProjectConfig.java         # ModelMapper, BCryptPasswordEncoder beans
│   └── SecurityConfig.java        # Filter chain, OAuth2 client, permit rules
│
├── controllers/
│   ├── AuthController.java        # /api/v1/auth — login, register, refresh, logout
│   └── UserController.java        # /api/v1/users — CRUD (ADMIN only)
│
├── dtos/
│   ├── LoginRequest.java          # record { email, password }
│   ├── RefreshTokenRequest.java   # record { refreshToken }
│   ├── TokenResponse.java         # accessToken, refreshToken, expiresIn, user
│   ├── UserDto.java               # User data transfer object
│   ├── RoleDto.java
│   ├── ApiError.java
│   └── ErrorResponse.java
│
├── entities/
│   ├── User.java                  # id (UUID), email, name, image, password, enable, provider, roles
│   ├── Role.java                  # id, name
│   ├── RefreshToken.java          # jti, user, createdAt, expiresAt, revoked, replacedByToken
│   └── Provider.java              # LOCAL | GOOGLE (enum)
│
├── exceptions/
│   ├── GlobalExceptionHandler.java   # @RestControllerAdvice → ErrorResponse
│   └── ResourceNotFoundException.java
│
├── helpers/
│   └── UserHelper.java            # Shared user-construction utilities
│
├── repositories/
│   ├── UserRepository.java        # findByEmail
│   ├── RoleRepository.java        # findByName
│   └── RefreshTokenRepository.java # findByJti
│
├── security/
│   ├── JwtService.java            # generateAccessToken, generateRefreshToken, parse, isAccessToken, isRefreshToken
│   ├── JwtAuthenticationFilter.java # OncePerRequestFilter — extracts + validates access token
│   ├── CustomUserDetailService.java # loadUserByUsername → UserDetails
│   └── Oauth2SuccessHandler.java  # Post-Google-auth: provision user + issue JWT
│
└── services/
    ├── AuthService.java           # interface: registerUser
    ├── UserService.java           # interface: CRUD operations
    └── Impl/
        ├── AuthServiceImpl.java   # registerUser → hash password, assign GUEST, save
        ├── UserServiceImpl.java   # CRUD impl with ModelMapper
        └── CookieService.java     # attachRefreshCookie, clearRefreshCookie, addNoStoreHeaders
```

---

## Data Model

```
Role ──────< User >────── RefreshToken
              │
              └── Provider (LOCAL | GOOGLE)
```

| Table | Key Columns | Notes |
|-------|------------|-------|
| `users` | `id (UUID PK)`, `email (unique)`, `password`, `name`, `image`, `enable`, `provider` | Password is null for OAuth2 users |
| `roles` | `id`, `name` | Seeded: `ADMIN`, `GUEST` |
| `user_roles` | `user_id`, `role_id` | Join table (ManyToMany) |
| `refresh_tokens` | `id`, `jti (unique)`, `user_id (FK)`, `created_at`, `expires_at`, `revoked`, `replaced_by_token` | One row per active session; rotated on every refresh |

---

## Security Design

### JWT Structure

**Access token claims:**

```json
{
  "jti": "<random UUID>",
  "sub": "<user UUID>",
  "iss": "api.substring.com",
  "iat": 1710000000,
  "exp": 1710000600,
  "email": "user@example.com",
  "roles": ["GUEST"],
  "typ": "access"
}
```

**Refresh token claims (minimal, for security):**

```json
{
  "jti": "<stored JTI>",
  "sub": "<user UUID>",
  "iss": "api.substring.com",
  "iat": 1710000000,
  "exp": 1710086400,
  "typ": "refresh"
}
```

Both are signed with `HS512`. The secret must be ≥ 64 characters (validated in `JwtService` constructor).

### Request filter chain

```
Request
  └─▶ JwtAuthenticationFilter
        ├── read Authorization: Bearer <token>
        ├── JwtService.isAccessToken(token)  ← rejects refresh tokens used as access
        ├── getUserId + getEmail + getRoles
        ├── build UsernamePasswordAuthenticationToken
        └── SecurityContextHolder.setAuthentication(...)
  └─▶ Controller
```

### Refresh token rotation

```
Client sends refresh token (cookie / body / header)
  └─▶ JwtService.isRefreshToken(token)
  └─▶ get JTI → look up RefreshToken row
  └─▶ check: not revoked, not expired, userId matches
  └─▶ mark old token revoked; record replacedByToken = newJti
  └─▶ create new RefreshToken row in DB
  └─▶ generate new access + refresh tokens
  └─▶ set new refresh token in cookie
  └─▶ return TokenResponse
```

A stolen and replayed refresh token will be rejected because the original is already marked `revoked=true` after the legitimate client rotated it.

### Cookie settings (all configurable via `application-dev.yml`)

| Setting | Default |
|---------|---------|
| `HttpOnly` | `true` — JS cannot read the cookie |
| `Secure` | `true` — HTTPS only (set `false` for local HTTP dev) |
| `SameSite` | `Lax` |
| `Domain` | `localhost` (override for production) |

---

## API Reference

Base path: `/api/v1`

### Auth Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/auth/register` | None | Register new user, returns `UserDto` |
| `POST` | `/auth/login` | None | Login with email/password, returns `TokenResponse` + sets refresh cookie |
| `POST` | `/auth/refresh` | Refresh token (cookie / body / header) | Rotate refresh token, return new `TokenResponse` |
| `POST` | `/auth/logout` | Refresh token (cookie) | Revoke token, clear cookie, `204 No Content` |

**`POST /auth/login` — request:**

```json
{
  "email": "user@example.com",
  "password": "secret"
}
```

**`TokenResponse` — response:**

```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn": 600,
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "name": "John",
    "roles": [{ "name": "GUEST" }]
  }
}
```

### Social Login

| Step | Action |
|------|--------|
| 1 | Redirect browser to `GET /oauth2/authorization/google` |
| 2 | Google login + consent |
| 3 | Spring redirects to `/login/oauth2/code/google` callback |
| 4 | `Oauth2SuccessHandler` issues JWT; response body = `"Login Successful"` + refresh cookie set |

### User Endpoints (ADMIN role required)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/users/` | List all users |
| `POST` | `/users/` | Create user |
| `GET` | `/users/{userId}` | Get user by ID |
| `GET` | `/users/email/{email}` | Get user by email |
| `POST` | `/users/{userId}` | Update user |
| `DELETE` | `/users/{userId}` | Delete user |

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3+
- MariaDB or MySQL running locally

### 1 — Clone and enter

```bash
git clone https://github.com/guptaaayansh/auth-backend.git
cd auth-backend
```

### 2 — Create the database

```sql
CREATE DATABASE auth_app;
CREATE USER 'auth_app'@'localhost' IDENTIFIED BY 'yourpassword';
GRANT ALL PRIVILEGES ON auth_app.* TO 'auth_app'@'localhost';
```

### 3 — Configure

Edit `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/auth_app
    username: auth_app
    password: yourpassword
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}

security:
  jwt:
    secret: ${JWT_SECRET}          # must be >= 64 characters
    access-ttl-seconds: 600
    refresh-ttl-seconds: 86400
```

### 4 — Run

```bash
./mvnw spring-boot:run
```

App starts on port `8083` (dev profile). Swagger UI: `http://localhost:8083/swagger-ui.html`

---

## Configuration

All sensitive values accept environment variable overrides (fall back to dev defaults):

| Property | Env var | Default (dev only) |
|----------|---------|-------------------|
| `security.jwt.secret` | `JWT_SECRET` | Long dev string |
| `security.jwt.access-ttl-seconds` | `JWT_ACCESS_TTL_SECONDS` | `600` |
| `security.jwt.refresh-ttl-seconds` | `JWT_REFRESH_TTL_SECONDS` | `86400` |
| `security.jwt.issuer` | `JWT_ISSUER` | `api.substring.com` |
| `security.jwt.cookie-secure` | `JWT_COOKIE_SECURE` | `true` |
| `security.jwt.cookie-domain` | `JWT_COOKIE_DOMAIN` | `localhost` |
| `spring.security.oauth2…google.client-id` | `GOOGLE_CLIENT_ID` | — |
| `spring.security.oauth2…google.client-secret` | `GOOGLE_CLIENT_SECRET` | — |

> **Production:** always override `JWT_SECRET` with a cryptographically random string of 64+ characters. Never ship the dev default.

---

## Environment Profiles

Three Spring profiles are included:

| Profile | File | Port | Database |
|---------|------|------|----------|
| `dev` | `application-dev.yml` | `8083` | MariaDB local |
| `qa` | `application-qa.yml` | — | Override as needed |
| `prod` | `application-prod.yml` | — | Override as needed |

Active profile is set in `application.yaml`:

```yaml
spring:
  profiles:
    active: dev
```

---

## Token Flow Diagrams

### Local Login

```
Client                           Server
  │                                │
  │── POST /auth/login ──────────▶ │
  │   { email, password }          │
  │                                │── AuthenticationManager.authenticate()
  │                                │── load User from DB
  │                                │── create RefreshToken row (JTI = UUID)
  │                                │── generateAccessToken(user)
  │                                │── generateRefreshToken(user, JTI)
  │◀── 200 TokenResponse ─────────  │
  │   Set-Cookie: refreshToken=... │
```

### Token Refresh

```
Client                           Server
  │                                │
  │── POST /auth/refresh ────────▶ │  (cookie auto-sent)
  │                                │── read JTI from token
  │                                │── look up RefreshToken row
  │                                │── check: not revoked, not expired
  │                                │── mark old token revoked
  │                                │── create new RefreshToken row
  │                                │── generate new access + refresh tokens
  │◀── 200 TokenResponse ─────────  │
  │   Set-Cookie: refreshToken=... │  (new cookie)
```

### Google OAuth2

```
Client                      Spring Boot              Google
  │                              │                      │
  │── GET /oauth2/auth/google ──▶│                      │
  │◀── 302 redirect ────────────│                      │
  │─────────────────────────────────────────────────▶  │
  │  (Google login + consent)                           │
  │◀──────────────────────────── 302 /login/oauth2/code/google
  │── GET /login/oauth2/code/google?code=... ─────────▶│
  │                              │── exchange code ────▶│
  │                              │◀── user profile ─────│
  │                              │── Oauth2SuccessHandler
  │                              │── upsert User row
  │                              │── issue JWT pair
  │◀── 200 "Login Successful" ──│
  │   Set-Cookie: refreshToken=...
```
