package dataaccess;

import static org.junit.jupiter.api.Assertions.*;

import exception.ResponseException;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SQLAuthDataAccessTest {
    private SQLUserDataAccess userDao;
    private SQLAuthDataAccess authDao;

    @BeforeEach
    public void setUp() throws ResponseException {
        userDao = new SQLUserDataAccess();
        authDao = new SQLAuthDataAccess();
        authDao.clear();
        userDao.clear();
        userDao.createUser(new UserData("alice", "pass", "alice@example.com"));
    }

    @Test
    public void clearSuccess() throws ResponseException {
        authDao.createAuth(new AuthData("alice", "token-abc"));
        authDao.clear();
        assertNull(authDao.getAuth("token-abc"));
    }

    @Test
    public void clearEmptyTableSuccess() {
        assertDoesNotThrow(() -> authDao.clear());
    }

    @Test
    public void createAuthSuccess() throws ResponseException {
        AuthData auth = authDao.createAuth(new AuthData("alice", "token-123"));
        assertNotNull(auth);
        assertEquals("alice", auth.getUsername());
    }

    @Test
    public void createAuthDuplicateTokenThrows() throws ResponseException {
        authDao.createAuth(new AuthData("alice", "dup-token"));
        assertThrows(ResponseException.class,
                () -> authDao.createAuth(new AuthData("alice", "dup-token")));
    }

    @Test
    public void getAuthSuccess() throws ResponseException {
        authDao.createAuth(new AuthData("alice", "token-xyz"));
        AuthData found = authDao.getAuth("token-xyz");
        assertNotNull(found);
        assertEquals("alice", found.getUsername());
    }

    @Test
    public void getAuthNotFoundReturnsNull() throws ResponseException {
        assertNull(authDao.getAuth("no-such-token"));
    }

    @Test
    public void deleteAuthSuccess() throws ResponseException {
        authDao.createAuth(new AuthData("alice", "token-del"));
        authDao.deleteAuth("token-del");
        assertNull(authDao.getAuth("token-del"));
    }

    @Test
    public void deleteAuthNonexistentDoesNotThrow() {
        assertDoesNotThrow(() -> authDao.deleteAuth("ghost-token"));
    }
}
