package cn.chper.avalon.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class UserService {

    private static HashMap<String, String> users = new HashMap<>();

    private static HashMap<String, String> tokens = new HashMap<>();

    public boolean login(String username, String password) {
        if (!users.containsKey(username)) {
            users.put(username, password);
            return true;
        }
        return users.get(username).equals(password);
    }

    public String getToken(String username) {
        if (tokens.containsKey(username)) {
            return tokens.get(username);
        }
        String token = RandomStringUtils.randomAlphanumeric(20);
        tokens.put(username, token);
        return token;
    }

    public static String getUsernameByToken(String token) {
        for (String username : tokens.keySet()) {
            if (tokens.get(username).equals(token)) {
                return username;
            }
        }
        return null;
    }

}
