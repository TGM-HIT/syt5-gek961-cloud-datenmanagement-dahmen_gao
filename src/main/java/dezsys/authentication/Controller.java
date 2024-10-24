package dezsys.authentication;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
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

@RestController
@RequestMapping("/auth")
public class Controller {
    private static final String initialUsersJson = "InitialUsers.json";
    private SecretKey key = Jwts.SIG.HS256.key().build();
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
            // Save users to the repository
            repo.saveAll(List.of(users));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @PostMapping("/admin/register")
    public String register(@RequestBody RegisterRequest req) {
        String name = req.name();
        String email = req.email();
        var roles = req.roles();
        String password = req.password();

        System.out.println(String.format("name: %s, email: %s, roles: %s", name, email,
                roles.stream().map(Role::name).collect(Collectors.joining(", "))));

        // TODO check if roles include ADMIn

        // TODO create user

        return createJwt(email, password);
    }

    @PostMapping("/signin")
    public String signin(@RequestBody LoginRequest req) {
        System.out.println(req.toString());

        String email = req.email();
        String password = req.password();

        // TODO check login creds

        return createJwt(email, password);
    }

    @GetMapping("/verify")
    public String verify(@RequestHeader("Authorization") String token) {
        System.out.println("token: " + token);
        String jwt = token.replace("Bearer: ", "");
        parseJwt(jwt);
        return "verify";
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
