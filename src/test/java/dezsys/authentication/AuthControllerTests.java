
package dezsys.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.File;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    // src for jwt: https://jwt.io/
    private static final String INVALID_JWT = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    private String adminJwt = "Bearer noTokenYet";

    private MyUser adminUser;

    @BeforeEach
    public void retrieveAdminJwt() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        MyUser[] users = objectMapper.readValue(new File("InitialUsers.json"), MyUser[].class);

        assertTrue(users.length >= 1, "InitialUsers.json has no users");
        assertTrue(users[0].roles.contains(Role.ADMIN), "initial user is no admin");

        adminUser = users[0];

        String requestBody = createRequestBody(adminUser.email, adminUser.password);

        String response = mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract JWT from the response (adjust the path if your response format is
        // different)
        // adminJwt = "Bearer " + response.replaceAll(".*\"jwt\":\"([^\"]+)\".*", "$1");
        adminJwt = "Bearer " + response;
        System.out.printf("got admin jwt %s\n", adminJwt);
    }

    @Test
    public void testAdminRegister_withValidJwt_shouldSucceed() throws Exception {
        String body = createRequestBody("johndoe2@example.com", "aoeuhtnsHTNS123!!");
        mockMvc.perform(post("/auth/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", adminJwt)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("registration pending"));
    }

    @Test
    public void testAdminRegister_withInvalidJwt_shouldFail() throws Exception {
        String requestBody = createRequestBody("johndoe2@example.com", "aoeuhtnsHTNS123!!");

        mockMvc.perform(post("/auth/admin/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", INVALID_JWT)
                .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSignin_withValidCredentials_shouldReturnJwt() throws Exception {

        String requestBody = createRequestBody(adminUser.email, adminUser.password);

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    public void testSignin_withInvalidCredentials_shouldFail() throws Exception {
        String requestBody = createRequestBody(adminUser.email, adminUser.password + "uwu");

        mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testVerify_withValidJwt_shouldSucceed() throws Exception {
        mockMvc.perform(get("/auth/verify")
                .header("Authorization", adminJwt))
                .andExpect(status().isOk());
    }

    @Test
    public void testVerify_withInvalidJwt_shouldFail() throws Exception {
        mockMvc.perform(get("/auth/verify")
                .header("Authorization", INVALID_JWT))
                .andExpect(status().isForbidden());
    }

    private String createRequestBody(String email, String password) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        MyUser user = new MyUser(email, "John Doe", List.of(Role.ADMIN), password);
        return om.writeValueAsString(user);
    }

}
