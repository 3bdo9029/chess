package dataaccess.sql;

import static org.junit.jupiter.api.Assertions.*;

import exception.ResponseException;
import org.junit.jupiter.api.Test;

public class SQLDataAccessTest {
    private static final String[] NO_TABLES = {};

    @Test
    public void constructorRunsCreateStatements() {
        assertDoesNotThrow(() -> new SQLDataAccess(NO_TABLES) {
        });
    }

    @Test
    public void constructorInvalidCreateStatementThrows() {
        ResponseException ex = assertThrows(ResponseException.class,
                () -> new SQLDataAccess(new String[] {"CREATE TABLE"}) {
                });
        assertEquals(500, ex.getStatusCode());
    }

    @Test
    public void executeUpdateInvalidSqlThrows() throws ResponseException {
        SQLDataAccess dao = new SQLDataAccess(NO_TABLES) {
        };
        ResponseException ex = assertThrows(ResponseException.class,
                () -> dao.executeUpdate("THIS IS NOT SQL"));
        assertEquals(500, ex.getStatusCode());
    }
}
