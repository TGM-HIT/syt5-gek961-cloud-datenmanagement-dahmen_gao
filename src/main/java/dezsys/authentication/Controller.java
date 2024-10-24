package dezsys.authentication;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

@RestController
@RequestMapping("/auth")
public class Controller {
    // get key for this running instance
    private SecretKey key = Jwts.SIG.HS256.key().build();

    @Autowired
    UserRepository repo;

    @RequestMapping("/user")
    public String createUser() {
        User u = new User(1L, "simon", "email", List.of(Role.ADMIN), "password");
        repo.save(u);
        return "done";
    }

    @RequestMapping("/admin/register")
    public String register(String name, String email, @RequestParam(value = "roles") ArrayList<Role> roles, String password) {
        System.out.println(String.format("name: %s, email: %s, roles: %s", name, email,
                roles.stream().map(Role::name).collect(Collectors.joining(", "))));
        return createJwt(email, password);
    }

    @PostMapping("/signin")
    public String signin(@RequestBody LoginRequest req) {
        System.out.println(req.toString());
        return "signin";
    }

    @RequestMapping("/verify")
    public String verify(String token) {
        System.out.println("token: " + token);
        parseJwt(token);
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

    record LoginRequest(String email, String password) {

    }

}
