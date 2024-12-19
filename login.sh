#!/bin/bash

# Frage nach der E-Mail (✿◠‿◠)
read -p "Please enter your email: " email

# Frage nach dem Passwort (Es wird nicht angezeigt während der Eingabe) ^_^
read -s -p "Please enter your password: " password

# Verschlüssele das Passwort mit bcrypt UwU
# encrypted_password=$(echo -n "$password" | bcrypt -o 10) # -o 10 für Work Factor 10

# encrypted_password=$(python3 -c "
# import bcrypt
# password = '$password'.encode('utf-8')
# hashed = bcrypt.hashpw(password, bcrypt.gensalt())
# print(hashed.decode('utf-8'))
# ")

# Führe den cURL-Befehl aus und sende die verschlüsselte E-Mail und Passwort als JSON (๑˃̵ᴗ˂̵)و
curl -X POST http://localhost:8080/auth/signin \
-H "Content-Type: application/json" \
-d "{\"email\":\"$email\", \"password\":\"$password\"}"
