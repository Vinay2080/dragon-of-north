# API Contracts

## Conventions
- Content-Type: `application/json`
- Response envelope: `{"status": "success|failed", "message": "...", "data": ...}`
- JSON naming: `snake_case`
- Cookie auth model: `access_token` + `refresh_token` cookies
- CSRF header (for protected mutating requests): `X-XSRF-TOKEN`

## Authentication + Account

### POST `/api/v1/auth/identifier/status`
- Auth: Public
- Headers: none
- Request:
```json
{ "identifier": "user@example.com", "identifier_type": "EMAIL" }
```
- Response `200`:
```json
{ "status": "success", "data": { "identifier": "user@example.com", "app_user_status": "CREATED" } }
```

### POST `/api/v1/auth/identifier/sign-up`
- Auth: Public
- Request:
```json
{ "identifier": "user@example.com", "identifier_type": "EMAIL", "password": "Example@123" }
```
- Response `201`: status payload in envelope

### POST `/api/v1/auth/identifier/sign-up/complete`
- Auth: Public
- Request:
```json
{ "identifier": "user@example.com", "identifier_type": "EMAIL" }
```
- Response `201`: status payload in envelope

### POST `/api/v1/auth/identifier/login`
- Auth: Public (CSRF bypassed)
- Request:
```json
{ "identifier": "user@example.com", "password": "Example@123", "device_id": "web-chrome-macos" }
```
- Response `200`: success message
- Side effects: sets `access_token` and `refresh_token` cookies

### POST `/api/v1/auth/jwt/refresh`
- Auth: Public (requires valid `refresh_token` cookie + `device_id`)
- Request:
```json
{ "device_id": "web-chrome-macos" }
```
- Response `200`: success message
- Side effects: rotates `refresh_token`, sets new `access_token`

### POST `/api/v1/auth/identifier/logout`
- Auth: Public endpoint but requires refresh-cookie context for meaningful revoke
- Request:
```json
{ "device_id": "web-chrome-macos" }
```
- Response `200`: success message
- Side effects: clears token cookies; revokes matching session

### POST `/api/v1/auth/password/forgot/request`
- Auth: Public
- Request:
```json
{ "identifier": "user@example.com", "identifier_type": "EMAIL" }
```
- Response `200`: generic reset-instructions message

### POST `/api/v1/auth/password/forgot/reset`
- Auth: Public
- Request:
```json
{ "identifier": "user@example.com", "identifier_type": "EMAIL", "otp": "123456", "new_password": "NewPass@123" }
```
- Response `200`: success message

### POST `/api/v1/auth/password/change`
- Auth: Required
- Headers: `X-XSRF-TOKEN`
- Request:
```json
{ "old_password": "OldPass@123", "new_password": "NewPass@123" }
```
- Response `200`: success message

### GET `/api/v1/auth/csrf`
- Auth: Public
- Response `200`:
```json
{ "status": "success", "data": { "token": "csrf-token-value" } }
```
- Side effects: sets `XSRF-TOKEN` cookie

## OAuth

### POST `/api/v1/auth/oauth/google`
- Auth: Public (CSRF bypassed)
- Request:
```json
{ "id_token": "google-id-token", "device_id": "web-chrome-macos", "expected_identifier": "user@example.com" }
```
- Response `200`: success message + auth cookies

### POST `/api/v1/auth/oauth/google/signup`
- Auth: Public (CSRF bypassed)
- Request: same shape as above
- Response `200`: success message + auth cookies

## OTP

### POST `/api/v1/otp/email/request`
- Auth: Public
- Request:
```json
{ "email": "user@example.com", "otp_purpose": "SIGNUP" }
```
- Response `201`: `"OTP sent"`

### POST `/api/v1/otp/phone/request`
- Auth: Public
- Request:
```json
{ "phone": "9876543210", "otp_purpose": "LOGIN" }
```
- Response `201`: `"OTP Sent"`

### POST `/api/v1/otp/email/verify`
- Auth: Public
- Request:
```json
{ "email": "user@example.com", "otp": "123456", "otp_purpose": "SIGNUP" }
```
- Response `202|400`: `OtpVerificationStatus` in envelope

### POST `/api/v1/otp/phone/verify`
- Auth: Public
- Request:
```json
{ "phone": "9876543210", "otp": "123456", "otp_purpose": "LOGIN" }
```
- Response `202|400`: `OtpVerificationStatus` in envelope

## Profile

### GET `/api/v1/profile`
- Auth: Required
- Headers: cookie `access_token`
- Response `200`:
```json
{ "status": "success", "data": { "username": "vinay", "display_name": "Vinay", "bio": "...", "avatar_url": "...", "provider": "LOCAL" } }
```

### PATCH `/api/v1/profile`
- Auth: Required
- Headers: `X-XSRF-TOKEN`
- Request:
```json
{ "username": "vinay", "display_name": "Vinay", "bio": "...", "avatar_url": "..." }
```
- Response `200`: success message

## Sessions

### GET `/api/v1/sessions/get/all`
- Auth: Required
- Response `200`: list of session summaries

### DELETE `/api/v1/sessions/delete/{session_id}`
- Auth: Required
- Headers: `X-XSRF-TOKEN`
- Response `200`: success message

### POST `/api/v1/sessions/revoke-others`
- Auth: Required
- Headers: `X-XSRF-TOKEN`
- Request:
```json
{ "device_id": "web-chrome-macos" }
```
- Response `200`: count-based revoke message
