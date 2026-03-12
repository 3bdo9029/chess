package dataaccess;

import static org.junit.jupiter.api.Assertions.*;

import exception.ResponseException;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SQLUserDataAccessTest {
    private SQLUserDataAccess userDao;

    @BeforeEach
    public void setUp() throws ResponseException {
        userDao = new SQLUserDataAccess();
        userDao.clear();
    }

    @Test
    public void clearSuccess() throws ResponseException {
        userDao.createUser(new UserData("alice", "pass", "alice@example.com"));
        userDao.clear();
        assertNull(userDao.getUser("alice"));
    }

    @Test
    public void clearEmptyTableSuccess() {
        assertDoesNotThrow(() -> userDao.clear());
    }

    @Test
    public void createUserSuccess() throws ResponseException {
        UserData created = userDao.createUser(new UserData("bob", "secret", "bob@example.com"));
        assertNotNull(created);
        assertEquals("bob", created.getUsername());
    }

    @Test
    public void createUserDuplicateThrows() throws ResponseException {
        userDao.createUser(new UserData("carol", "pass", "carol@example.com"));
        assertThrows(ResponseException.class,
                () -> userDao.createUser(new UserData("carol", "other", "carol2@example.com")));
    }

    @Test
    public void getUserSuccess() throws ResponseException {
        userDao.createUser(new UserData("dave", "pw", "dave@example.com"));
        UserData found = userDao.getUser("dave");
        assertNotNull(found);
        assertEquals("dave", found.getUsername());
    }

    @Test
    public void getUserNotFoundReturnsNull() throws ResponseException {
        assertNull(userDao.getUser("nobody"));
    }
}
