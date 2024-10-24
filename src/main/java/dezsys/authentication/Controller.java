package dezsys.authentication;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class Controller {

    @RequestMapping("/admin/register")
    public String register() {
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
