package dezsys.authentication;

import java.io.File;
import org.mindrot.jbcrypt.BCrypt;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RestController
@RequestMapping("/auth")
public class Controller {
    private static final String initialUsersJson = "InitialUsers.json";
    private SecretKey key = Jwts.SIG.HS256.key().build();
    // private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Autowired
    MyUserRepository repo;

    @PostConstruct
    public void init() {
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
    }


    @PostMapping("/admin/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest req, @RequestHeader("Authorization") String token) {
        String name = req.name();
        String email = req.email();
        var roles = req.roles();
        String password = req.password();

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

            // user gets created

            System.out.println(String.format("name: %s, email: %s, roles: %s", name, email,
                    roles.stream().map(Role::name).collect(Collectors.joining(", "))));

            var saltedHash = BCrypt.hashpw(password, BCrypt.gensalt());
            MyUser newEntity = new MyUser(email, name, roles, saltedHash);
            repo.save(newEntity);

            return ResponseEntity.ok("created user " + name);
        } catch (SignatureException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("bad jwt");
        }

    }

    @PostMapping("/signin")
    public ResponseEntity<String> signin(@RequestBody LoginRequest req) {
        System.out.println(req.toString());

        String email = req.email();
        String password = req.password();

        MyUser userEntity = repo.findById(email).orElse(null);
        if(userEntity == null) {
            // no user found
            System.out.println(String.format("no user with email %s found",email));
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("bad credentials");
        }

        // check if passwords match
        // if(!password.equals(userEntity.getPassword())) {
        // if(!passwordEncoder.matches(password, userEntity.getPassword())) {
        if(!BCrypt.checkpw(password, userEntity.getPassword())) {
            // password mismatch
            System.out.println(String.format("expected password %s but got %s", userEntity.getPassword(), password));
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
        } catch (SignatureException e) {
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

    record RegisterRequest(String name, String email, ArrayList<Role> roles,
            String password) {

    }

    record LoginRequest(String email, String password) {

    }

}
