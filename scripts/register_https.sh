#!/bin/bash

# read -p "Please enter admin email: " adminEmail
# read -s -p "Please enter admin password: " adminPassword

adminEmail=johndoe@example.com
adminPassword=securePassword123

# Frage nach der E-Mail (✿◠‿◠)
read -p "Please enter the new user's email: " email

# Frage nach dem Namen (Name für den neuen Benutzer)
read -p "Please enter the new user's name: " name

# Frage nach den Rollen für den neuen Benutzer (z.B. ADMIN, USER)
read -p "Please enter the new user's roles (comma-separated, ADMIN,MODERATOR,READER): " roles_input
IFS=',' read -r -a roles <<< "$roles_input"

# Frage nach dem Passwort (Es wird nicht angezeigt während der Eingabe) ^_^
read -s -p "Please enter the new user's password: " password

# Bereite die JSON-Daten vor, die an den Server gesendet werden
roles_json="["

for role in "${roles[@]}"; do
  roles_json+="\"$role\","
done

# Entferne das letzte Komma
roles_json="${roles_json%,}]"

jwt=$(curl -X POST http://localhost:8080/auth/signin \
-H "Content-Type: application/json" \
-d "{\"email\":\"$adminEmail\", \"password\":\"$adminPassword\"}")

# Führe den cURL-Befehl aus und sende die Daten als JSON
curl -k -X POST https://localhost:8443/auth/admin/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $jwt" \
  -d "{\"name\":\"$name\", \"email\":\"$email\", \"roles\":$roles_json, \"password\":\"$password\"}"
