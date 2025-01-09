# Middleware Engineering Login System

Felix Dahmen

Simon Gao

## Introduction

This is a spring boot application that provides endpoints for user registration, login and verification.
It uses JWT as a session token to authenticate users.

## Usage

To deploy: 

```bash
cd nginx/certs  # Switch into cert directory
./genkey.sh     # Execute the genkey script that generates a cert with openssl

cd ..
docker compose up
```

## API Documentation

Welcome to the API documentation! This document describes the available endpoints for user authentication and management.

### Endpoints

#### 1. `/auth/admin/register`
**Method:** `POST`  
**Description:** This endpoint is not implemented in this version yet. It will allow an admin to create new users.  
**Authentication:** Requires a logged-in admin with a valid JWT token.

#### 2. `/auth/signin`
**Method:** `POST`  
**Description:** This endpoint allows users to log in with their email and hashed password. On successful login, it returns a JWT token.  
**Request Body:**
```json
{
    "email": "user@example.com",
    "password": "hashed_password"
}
```

**Response:** a JWT 
**Method:** `GET`  
**Description:** This endpoint allows you to check if a provided JWT token is valid.
**Request Header:** `Authorization: Bearer your_jwt_token` (replace your_jwt_token with the token)

## Scripts

There are 2 scripts:

`login.sh` asks for email and password and returns JWT on successful login 
`verify.sh` asks for a JWT as input and verifies it.

The login is currently broken, because the imported user (from the json file) uses a wrong hashing algorithm 
that does not match the password hash which gets sent.

#### 3. `/auth/verify`

## Dependencies
Spring Web - Build web, including RESTful, applications using Spring MVC. Uses Apache Tomcat as
he default embedded container.

Spring Data JPA - Persist data in SQL stores with Java Persistence API using Spring Data and
Hibernate.

Spring Security - Highly customizable authentication and access-control framework for Spring applications.


## Extended task: Attack vectors

### 1. Detect Weak or improper passwords

There was no password strength validation.
This was fixed by ensuring that for registering, each password is at least 8 characters long and contains
- lowercase letters
- uppercase letters
- digits
- special characters

### 2. Switch to HTTPS

To include ssl/tls encryption for the login endpoints the `compose.yml` includes a nginx reverse proxy that uses a self-signed certificicate
to encrypt traffic, NOTE: The traffic between the reverse proxy and the spring-boot application is not encrypted, but it uses the internal docker network
that is separated.

Before deploying the application you have to generate the self-signed certificate, just do:
```console
cd nginx/certs # Switch into cert directory
./genkey.sh     # Execute the genkey script that generates a cert with openssl
```

### 3. Password hashing

Passwords should be stored as a salted hash in the database.
The hash is to prevent user passwords from getting leaked on data breaches, 
and salting is to prevent rainbow table attacks.
This is an attack where procompiled hashes are used to quickly check breached databases, 
to find out plain-text passwords.

### 4. Ambiguous Registration and Login

A malicious user could enter an email of another person, and find out if they have an account, by inspecting 
the error message or success.

To prevent this, the registration should never show messages like 'user already exists' (for registering),
or 'wrong password' (for login); but rather show 'registration pending' or 'wrong username or password'.

Also, the response should take the same amount of time, regardless of whether an user exists with the specific email.
This is done to counter an attacker being able to extract information from the time it takes to get the response.

### 5. Email confirmation

To ensure that accounts only are registered when an account exists a confirmation email with
a code in sent out.
