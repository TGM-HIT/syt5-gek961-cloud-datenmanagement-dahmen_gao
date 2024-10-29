# Middleware Engineering Login System

Felix Dahmen

Simon Gao

## Introduction

This is a spring boot application that provides endpoints for user registration, login and verification.
It uses JWT as a session token to authenticate users.

## Usage

To deploy: 

```bash
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

