package com.banchango.users;

import com.banchango.auth.token.JwtTokenUtil;
import com.banchango.domain.users.UserRole;
import com.banchango.domain.users.UserType;
import com.banchango.domain.users.Users;
import com.banchango.domain.users.UsersRepository;
import com.banchango.users.exception.UserEmailNotFoundException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserApiTest {

    private static final String TEST_EMAIL = "test@testing.com";

    @Before
    public void saveUser() {
        Users user = Users.builder().name("TEST_NAME")
                .email(TEST_EMAIL)
                .password("123")
                .type(UserType.OWNER)
                .telephoneNumber("010123123")
                .phoneNumber("010123123")
                .companyName("companyName")
                .role(UserRole.USER)
                .build();
        usersRepository.save(user);
    }

    @After
    public void removeUser() {
        Users user = usersRepository.findByEmail(TEST_EMAIL).orElseThrow(UserEmailNotFoundException::new);
        usersRepository.delete(user);
    }

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void userInfo_responseIsOk_IfAllConditionsAreRight() {
        Integer userId = getUserIdByEmail(TEST_EMAIL);
        String accessToken = JwtTokenUtil.generateAccessToken(userId, UserRole.USER);
        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/" + userId))
                .header("Authorization", "Bearer " + accessToken).build();
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONObject responseBody = new JSONObject(response.getBody());
        assertEquals(TEST_EMAIL, responseBody.get("email"));
        assertEquals(userId, responseBody.get("userId"));
        assertEquals("TEST_NAME", responseBody.get("name"));
        assertEquals("OWNER", responseBody.get("type"));
        assertEquals("010123123", responseBody.get("phoneNumber"));
        assertEquals("companyName", responseBody.get("companyName"));
        assertFalse(response.getBody().contains("password"));
    }

    @Test
    public void userInfo_responseIsUnAuthorized_IfTokenIsAbsent() {
        Integer userId = getUserIdByEmail(TEST_EMAIL);
        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/" + userId)).build();
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void userInfo_responseIsUnAuthorized_IfTokenIsMalformed() {
        Integer userId = getUserIdByEmail(TEST_EMAIL);
        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/" + userId)).build();
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void userInfo_responseIsNoContent_IfUserIdIsWrong() {
        String accessToken = JwtTokenUtil.generateAccessToken(0, UserRole.USER);
        RequestEntity<Void> request = RequestEntity.get(URI.create("/v3/users/0"))
                .header("Authorization", "Bearer " + accessToken).build();
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void signIn_responseIsOK_IfUserExists() {

        JSONObject requestBody = new JSONObject();
        requestBody.put("email", TEST_EMAIL);
        requestBody.put("password", "123");

        ResponseEntity<String> response = getResponse(requestBody.toString(), "/v3/users/sign-in");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("accessToken"));
        assertTrue(response.getBody().contains("refreshToken"));
        assertTrue(response.getBody().contains("tokenType"));
        assertTrue(response.getBody().contains("user"));

        JSONObject responseBody = new JSONObject(response.getBody());
        JSONObject userInfoResponseBody = responseBody.getJSONObject("user");

        assertEquals("TEST_NAME", userInfoResponseBody.get("name"));
        assertEquals(TEST_EMAIL, userInfoResponseBody.get("email"));
        assertEquals(UserType.OWNER.name(), userInfoResponseBody.get("type"));
        assertEquals("010123123", userInfoResponseBody.get("phoneNumber"));
        assertEquals("companyName", userInfoResponseBody.get("companyName"));
        assertEquals(UserRole.USER.name(), userInfoResponseBody.get("role"));
    }

    @Test
    public void signIn_responseIsNoContent_IfUserEmailIsWrong(){
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", "WRONG_EMAIL");
        requestBody.put("password", "123");

        ResponseEntity<String> response = getResponse(requestBody.toString(), "/v3/users/sign-in");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void signIn_responseIsNoContent_IfUserPasswordIsWrong() {
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", TEST_EMAIL);
        requestBody.put("password", "1234");

        ResponseEntity<String> response = getResponse(requestBody.toString(), "/v3/users/sign-in");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    }

    @Test
    public void signIn_responseIsBadRequest_IfRequestBodyIsWrong() {
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", TEST_EMAIL);
        requestBody.put("pass", "123");

        ResponseEntity<String> response = getResponse(requestBody.toString(), "/v3/users/sign-in");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void signUp_responseIsOK() {
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", "TEST_NAME");
        requestBody.put("email", "TEST_EMAIL_2");
        requestBody.put("password", "1234");
        requestBody.put("type", UserType.SHIPPER.name());
        requestBody.put("telephoneNumber", "010234234");
        requestBody.put("phoneNumber", "010234234");
        requestBody.put("companyName", "companyName");

        ResponseEntity<String> response = getResponse(requestBody.toString(), "/v3/users/sign-up");


        Users savedUser = usersRepository.findByEmail("TEST_EMAIL_2").orElseThrow(UserEmailNotFoundException::new);
        Integer userId = savedUser.getUserId();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONObject responseBody = new JSONObject(response.getBody());
        assertEquals("TEST_EMAIL_2", responseBody.get("email"));
        assertEquals(userId, responseBody.get("userId"));
        assertEquals("TEST_NAME", responseBody.get("name"));
        assertEquals("SHIPPER", responseBody.get("type"));
        assertEquals("010234234", responseBody.get("phoneNumber"));
        assertEquals("companyName", responseBody.get("companyName"));
        assertFalse(response.getBody().contains("password"));
        assertTrue(savedUser.getCreatedAt().isBefore(LocalDateTime.now()));
        assertTrue(savedUser.getLastModifiedAt().isBefore(LocalDateTime.now()));

        removeUserByUserId(userId);
    }

    @Test
    public void signUp_responseIsConflict_IfEmailExists() {

        JSONObject requestBody = new JSONObject();
        requestBody.put("name", "TEST_NAME");
        requestBody.put("email", TEST_EMAIL);
        requestBody.put("password", "1234");
        requestBody.put("type", UserType.SHIPPER.name());
        requestBody.put("phoneNumber", "010234234");
        requestBody.put("telephoneNumber", "010234234");
        requestBody.put("companyName", "companyName");

        ResponseEntity<String> response = getResponse(requestBody.toString(), "/v3/users/sign-up");

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    public void signUp_responseIsBadRequest_IfRequestBodyIsWrong() {
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", "TEST_NAME");
        requestBody.put("email", "TEST_EMAIL_");
        requestBody.put("password", "1234");
        requestBody.put("type", "WRONG_TYPE");
        requestBody.put("phoneNumber", "010234234");
        requestBody.put("companyName", "companyName");

        ResponseEntity<String> response = getResponse(requestBody.toString(), "/v3/users/sign-up");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void updateInfo_responseIsOk() {
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", "TEST_NAME_");
        requestBody.put("email", TEST_EMAIL);
        requestBody.put("password", "0000");
        requestBody.put("type", UserType.SHIPPER.name());
        requestBody.put("phoneNumber", "0101212");
        requestBody.put("telephoneNumber", "0101212");
        requestBody.put("companyName", "companyName");

        Integer userId = getUserIdByEmail(TEST_EMAIL);
        String accessToken = JwtTokenUtil.generateAccessToken(userId, UserRole.USER);

        RequestEntity<String> request = RequestEntity.patch(URI.create("/v3/users/" + userId))
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).body(requestBody.toString());

        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONObject responseBody = new JSONObject(response.getBody());
        assertEquals("TEST_NAME_", responseBody.get("name"));
        assertEquals(UserType.SHIPPER.name(), responseBody.get("type"));
        assertEquals("0101212", responseBody.get("phoneNumber"));
        assertEquals("companyName", responseBody.get("companyName"));
    }

    @Test
    public void updateInfo_responseIsUnAuthorized_IfTokenIsMalformed() {
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", "TEST_NAME_");
        requestBody.put("email", TEST_EMAIL);
        requestBody.put("password", "0000");
        requestBody.put("type", UserType.SHIPPER.name());
        requestBody.put("phoneNumber", "0101212");
        requestBody.put("companyName", "companyName");

        Integer userId = getUserIdByEmail(TEST_EMAIL);

        RequestEntity<String> request = RequestEntity.patch(URI.create("/v3/users/" + userId))
                .header("Authorization", "Bearer " + "THIS IS WRONG TOKEN!")
                .contentType(MediaType.APPLICATION_JSON).body(requestBody.toString());
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void updateInfo_responseIsUnAuthorized_IfUserIdAndTokenIsWrong() {
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", "TEST_NAME_");
        requestBody.put("email", TEST_EMAIL);
        requestBody.put("password", "0000");
        requestBody.put("type", UserType.SHIPPER.name());
        requestBody.put("telephoneNumber", "0101212");
        requestBody.put("phoneNumber", "0101212");
        requestBody.put("companyName", "companyName");

        Integer userId = getUserIdByEmail(TEST_EMAIL);
        String accessToken = JwtTokenUtil.generateAccessToken(userId, UserRole.USER);

        RequestEntity<String> request = RequestEntity.patch(URI.create("/v3/users/0"))
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).body(requestBody.toString());
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void updateInfo_responseIsNoContent_IfUserIdIsWrong() {
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", "TEST_NAME_");
        requestBody.put("email", TEST_EMAIL);
        requestBody.put("password", "0000");
        requestBody.put("type", UserType.SHIPPER.name());
        requestBody.put("telephoneNumber", "0101212");
        requestBody.put("phoneNumber", "0101212");
        requestBody.put("companyName", "companyName");

        String accessToken = JwtTokenUtil.generateAccessToken(0, UserRole.USER);

        RequestEntity<String> request = RequestEntity.patch(URI.create("/v3/users/0"))
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).body(requestBody.toString());
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    private ResponseEntity<String> getResponse(String requestBody, String url) {
        RequestEntity<String> request = RequestEntity.post(URI.create(url))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody);

        return restTemplate.exchange(request, String.class);
    }

    private Integer getUserIdByEmail(String email) {
        return usersRepository.findByEmail(email).orElseThrow(UserEmailNotFoundException::new).getUserId();
    }

    private void removeUserByUserId(Integer userId) {
        usersRepository.deleteById(userId);
    }
}
