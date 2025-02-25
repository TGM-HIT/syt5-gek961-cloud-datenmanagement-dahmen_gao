package dezsys.authentication;

import java.io.File;
import org.mindrot.jbcrypt.BCrypt;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import com.fasterxml.jackson.databind.ObjectMapper;

import dezsys.mail.EmailSender;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.MalformedJwtException;

@RestController
@RequestMapping("/auth")
public class Controller {
    private static final String initialUsersJson = "InitialUsers.json";
    private SecretKey key = Jwts.SIG.HS256.key().build();
    // private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Autowired
    MyUserRepository repo;

    private ConcurrentHashMap<String, MyUser> pendingRegistrations = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> emailToId = new ConcurrentHashMap<>();
    private final java.util.concurrent.ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private String host;
    private boolean isHttps;

    private EmailSender emailSender;

    @PostConstruct
    public void init() {
        this.host = System.getenv("HOST");
        this.isHttps = System.getenv("IS_HTTPS").equals("true");

        File usersJsonFile = new File(initialUsersJson);
        if (!usersJsonFile.exists()) {
            throw new RuntimeException("InitialUsers.json not found");
        }

        try {
            // Parse JSON string to User array
            ObjectMapper objectMapper = new ObjectMapper();
            MyUser[] users = objectMapper.readValue(usersJsonFile, MyUser[].class);
            users = Arrays.stream(users).map((MyUser u) -> {
                // u.password = passwordEncoder.encode(u.password);
                var salt = BCrypt.gensalt();
                u.password = BCrypt.hashpw(u.password, salt);
                return u;
            }).toArray(MyUser[]::new);
            // Save users to the repository
            repo.saveAll(List.of(users));
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.emailSender = new EmailSender("smtp.gmail.com", 587, System.getenv("EMAIL_USERNAME"), System.getenv("EMAIL_PASSWORD"), true);
    }

    @GetMapping("/register/verify/{verifyId}")
    public ResponseEntity<String> verifyRegister(@PathVariable String verifyId) {
        //  only allow creating ones that havent been scheduled
        MyUser newEntity = this.pendingRegistrations.get(verifyId);
        this.pendingRegistrations.remove(verifyId);
        String correctId = this.emailToId.get(newEntity.email);

        if(newEntity == null || !correctId.equals(verifyId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("invalid id");
        }

        this.emailToId.remove(newEntity.email);
        repo.save(newEntity);
        System.out.println("created user " + newEntity.name);
        return ResponseEntity.ok("created user");
    }

    @PostMapping("/admin/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest req, @RequestHeader("Authorization") String token) {
        String name = req.name();
        String email = req.email();
        var roles = req.roles();
        String password = req.password();

        // check password strength
        if(!isPasswordStrong(password)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("bad password strength");
        }

        // check if the token is valid and belongs to an admin
        String jwt = token.replace("Bearer ", "");
        try {
            var jws = parseJwt(jwt);
            String jwtEmail = (String)jws.getPayload().get("id");

            // check if token belongs to admin
            MyUser userEntity = repo.findById(jwtEmail).orElse(null);
            if(userEntity == null) {
                // no user found
                System.out.println(String.format("no user with email %s found",email));
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("bad credentials");
            }
            if(!userEntity.roles.contains(Role.ADMIN)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("no admin :c");
            }

            boolean success = true;

            // check if user already exists
            MyUser existingUser = repo.findById(email).orElse(null);
            if(existingUser != null) {
                System.out.println("user already exists! returning generic response");
                success = false;
            }

            // user gets created
            System.out.println(String.format("name: %s, email: %s, roles: %s", name, email,
                    roles.stream().map(Role::name).collect(Collectors.joining(", "))));

            System.out.println("creating user " + name);

            var saltedHash = BCrypt.hashpw(password, BCrypt.gensalt());
            if(success) {
                String registrationToken = UUID.randomUUID().toString();
                MyUser newEntity = new MyUser(email, name, roles, saltedHash);
                this.pendingRegistrations.put(registrationToken, newEntity);
                this.emailToId.put(email, registrationToken);
                scheduler.schedule(() -> {
                    this.pendingRegistrations.remove(registrationToken);
                    synchronized(this) {
                        String mappedToken = this.emailToId.getOrDefault(email, "");
                        if(mappedToken.equals(registrationToken)) {
                            this.emailToId.remove(email);
                        }
                    }
                    System.out.println("removed " + registrationToken + " for email " + email);
                }, 1, TimeUnit.MINUTES);
                String subject = "verify account " + name;
                String content = "click on " + (this.isHttps ? "https" : "http") + String.format("://%s/auth/register/verify/%s", this.host, registrationToken);
                try {
                    this.emailSender.sendEmail("spring dezsys application", System.getenv("EMAIL_USERNAME"), email, subject, content);
                }
                catch(Exception ex) {
                    System.err.println("failed sending email");
                    ex.printStackTrace();
                }
                System.out.println("path /auth/register/verify/" + registrationToken);
            }

            return ResponseEntity.ok("registration pending");
        } catch (SignatureException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("bad jwt");
        }

    }

    @PostMapping("/signin")
    public ResponseEntity<String> signin(@RequestBody LoginRequest req) {
        System.out.println(req.toString());

        String email = req.email();
        String password = req.password();

        boolean success = true;

        MyUser userEntity = repo.findById(email).orElse(null);
        if(userEntity == null) {
            // no user found
            System.out.println(String.format("no user with email %s found",email));
            success = false;
        }

        // check if passwords match
        // if(!password.equals(userEntity.getPassword())) {
        // if(!passwordEncoder.matches(password, userEntity.getPassword())) {
        if(userEntity != null && !BCrypt.checkpw(password, userEntity.getPassword())) {
            // password mismatch
            System.out.println(String.format("expected password %s but got %s", userEntity.getPassword(), password));
            success = false;
        }

        if(!success) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("bad credentials");
        }

        var token =createJwt(email, password);
        System.out.println("success sign in, token: " + token);
        return ResponseEntity.ok(token);
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestHeader("Authorization") String token) {
        System.out.println("token: " + token);
        String jwt = token.replace("Bearer ", "");
        try {
            parseJwt(jwt);
            return ResponseEntity.ok("valid");
        } catch (SignatureException | MalformedJwtException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("bad jwt");
        }
    }

    public String createJwt(String email, String password) {

        return Jwts.builder()
                .claim("id", email)
                .claim("password", password)
                .issuer("server")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(10, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parseJwt(String jwtString) {
        Jws<Claims> jwt = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwtString);
        System.out.println(jwt.getPayload().get("id"));
        System.out.println(jwt.getPayload().get("password"));

        return jwt;
    }

    public static boolean isPasswordStrong(String password) {
        // Minimum length: 8 characters
        if (password.length() < 8) {
            return false;
        }

        // Regular expressions for different criteria
        String upperCasePattern = ".*[A-Z].*"; // At least one uppercase letter
        String lowerCasePattern = ".*[a-z].*"; // At least one lowercase letter
        String digitPattern = ".*\\d.*";       // At least one digit
        String specialCharPattern = ".*[!@#$%^&*(),.?\":{}|<>].*"; // At least one special character

        // Validate all criteria
        return password.matches(upperCasePattern) &&
               password.matches(lowerCasePattern) &&
               password.matches(digitPattern) &&
               password.matches(specialCharPattern);
    }

    record RegisterRequest(String name, String email, ArrayList<Role> roles,
            String password) {

    }

    record LoginRequest(String email, String password) {

    }

}
