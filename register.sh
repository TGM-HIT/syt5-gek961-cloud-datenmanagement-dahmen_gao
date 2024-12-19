#!/bin/bash

# Frage nach der E-Mail (✿◠‿◠)
read -p "Please enter the new user's email: " email

# Frage nach dem Namen (Name für den neuen Benutzer)
read -p "Please enter the new user's name: " name

# Frage nach den Rollen für den neuen Benutzer (z.B. ADMIN, USER)
read -p "Please enter the new user's roles (comma-separated, ADMIN,MODERATOR,READER): " roles_input
IFS=',' read -r -a roles <<< "$roles_input"

# Frage nach dem Passwort (Es wird nicht angezeigt während der Eingabe) ^_^
read -s -p "Please enter the new user's password: " password

# Frage nach dem Admin-Token (Authentifizierung)
read -p "Please enter your admin JWT token: " token

# Bereite die JSON-Daten vor, die an den Server gesendet werden
roles_json="["

for role in "${roles[@]}"; do
  roles_json+="\"$role\","
done

# Entferne das letzte Komma
roles_json="${roles_json%,}]"

# Führe den cURL-Befehl aus und sende die Daten als JSON
response=$(curl -s -o response.txt -w "%{http_code}" -X POST http://localhost:8080/auth/admin/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $token" \
  -d "{\"name\":\"$name\", \"email\":\"$email\", \"roles\":$roles_json, \"password\":\"$password\"}")

# Ausgabe der Antwort vom Server
if [[ "$response" == "200" ]]; then
  echo "User created successfully!"
else
  echo "Failed to create user. Response: $response"
  cat response.txt
fi

