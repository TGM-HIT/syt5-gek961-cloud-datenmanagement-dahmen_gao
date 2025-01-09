openssl req -x509 -newkey rsa:4096 -keyout nginx.key -out nginx.crt -days 365 -nodes -subj "/CN=localhost"
