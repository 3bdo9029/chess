package dataAccess;

import java.util.HashMap;
import java.util.Map;
import model.UserData;

public class MemoryUserDataAccess implements UserDataAccess {
    private Map<String, UserData> users = new HashMap<String, UserData>();

    // Clears all users.
    public void clear() {
        users.clear();
    }

    // Create a new user.
    public UserData createUser(UserData user) {
        users.put(user.getUsername(), user);
        return user;
    }

    // Retrieve a user with the given username.
    public UserData getUser(String username) {
        return users.get(username);
    }
}
