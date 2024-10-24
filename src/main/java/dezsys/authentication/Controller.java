package dezsys.authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class Controller {

    @RequestMapping("/admin/register")
    public String register(String name, String email, @RequestParam(value = "roles")ArrayList<Role> roles) {
        System.out.println(String.format("name: %s, email: %s, roles: %s", name, email,
                roles.stream().map(Role::name).collect(Collectors.joining(", "))));
        return "register";
    }

    @RequestMapping("/signin")
    public String signin() {
        return "signin";
    }

    @RequestMapping("/verify")
    public String verify() {
        return "verify";
    }
}
