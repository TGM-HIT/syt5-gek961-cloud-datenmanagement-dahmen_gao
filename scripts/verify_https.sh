#!/bin/bash

# Frage nach dem JWT Token (⁄ ⁄•⁄ω⁄•⁄ ⁄)
read -p "Please enter your JWT token: " token

# Führe den cURL-Befehl mit dem eingegebenen Token aus ^_^
curl -X GET https://localhost:8443/auth/verify \
-H "Authorization: Bearer $token"
