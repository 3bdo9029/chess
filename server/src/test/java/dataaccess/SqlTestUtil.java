package dataaccess;

import java.sql.Statement;

/** Helpers that put the database into states the DAOs must handle as errors. */
final class SqlTestUtil {
    private SqlTestUtil() {
    }

    /** Drops the given tables in order. The next DAO constructor recreates them. */
    static void dropTables(String... tables) throws Exception {
        try (var conn = DatabaseManager.getConnection(); var stmt = conn.createStatement()) {
            for (var table : tables) {
                stmt.executeUpdate("DROP TABLE IF EXISTS " + table);
            }
        }
    }

    /** Inserts a game row whose JSON column cannot be deserialized and returns its id. */
    static int insertCorruptGame() throws Exception {
        var sql = "INSERT INTO game (whiteUsername, blackUsername, gameName, json) "
                + "VALUES (NULL, NULL, 'Broken', '{oops')";
        try (var conn = DatabaseManager.getConnection(); var stmt = conn.createStatement()) {
            stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            try (var rs = stmt.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
