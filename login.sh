#!/bin/sh
curl -X POST http://localhost:8080/auth/signin \
-H "Content-Type: application/json" \
-d '{"email": "cute@example.com", "password": "super_secret_password"}'
