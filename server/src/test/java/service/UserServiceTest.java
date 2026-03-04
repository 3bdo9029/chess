package service;

import static org.junit.jupiter.api.Assertions.*;

import dataaccess.MemoryAuthDataAccess;
import dataaccess.MemoryUserDataAccess;
import exception.ResponseException;
import model.AuthData;
import model.LoginRequest;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserServiceTest {
    private MemoryUserDataAccess userDataAccess;
    private MemoryAuthDataAccess authDataAccess;
    private UserService userService;

    @BeforeEach
    public void setUp() {
        userDataAccess = new MemoryUserDataAccess();
        authDataAccess = new MemoryAuthDataAccess();
        userService = new UserService(userDataAccess, authDataAccess);
    }

    @Test
    public void registerUserSuccess() throws ResponseException {
        UserData user = new UserData("alice", "password123", "alice@example.com");
        AuthData auth = userService.registerUser(user);

        assertNotNull(auth);
        assertEquals("alice", auth.getUsername());
        assertNotNull(auth.getAuthToken());
    }

    @Test
    public void registerUserNullUsernameFails() {
        UserData user = new UserData(null, "password123", "alice@example.com");
        ResponseException ex = assertThrows(ResponseException.class, () -> userService.registerUser(user));
        assertEquals(400, ex.getStatusCode());
    }

    @Test
    public void registerUserNullPasswordFails() {
        UserData user = new UserData("alice", null, "alice@example.com");
        ResponseException ex = assertThrows(ResponseException.class, () -> userService.registerUser(user));
        assertEquals(400, ex.getStatusCode());
    }

    @Test
    public void registerDuplicateUserFails() throws ResponseException {
        UserData user = new UserData("alice", "password123", "alice@example.com");
        userService.registerUser(user);
        ResponseException ex = assertThrows(ResponseException.class, () -> userService.registerUser(user));
        assertEquals(403, ex.getStatusCode());
    }

    @Test
    public void loginUserSuccess() throws ResponseException {
        UserData user = new UserData("alice", "password123", "alice@example.com");
        userService.registerUser(user);

        LoginRequest req = new LoginRequest("alice", "password123");
        AuthData auth = userService.loginUser(req);

        assertNotNull(auth);
        assertEquals("alice", auth.getUsername());
        assertNotNull(auth.getAuthToken());
    }

    @Test
    public void loginUserWrongPasswordFails() throws ResponseException {
        UserData user = new UserData("alice", "password123", "alice@example.com");
        userService.registerUser(user);

        LoginRequest req = new LoginRequest("alice", "wrongpassword");
        ResponseException ex = assertThrows(ResponseException.class, () -> userService.loginUser(req));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    public void loginUserNotExistFails() {
        LoginRequest req = new LoginRequest("ghost", "password");
        ResponseException ex = assertThrows(ResponseException.class, () -> userService.loginUser(req));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    public void logoutUserSuccess() throws ResponseException {
        UserData user = new UserData("alice", "password123", "alice@example.com");
        AuthData auth = userService.registerUser(user);

        assertDoesNotThrow(() -> userService.logoutUser(auth.getAuthToken()));
        assertNull(authDataAccess.getAuth(auth.getAuthToken()));
    }

    @Test
    public void logoutInvalidTokenFails() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> userService.logoutUser("invalid-token"));
        assertEquals(401, ex.getStatusCode());
    }
}
