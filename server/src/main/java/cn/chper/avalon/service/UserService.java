package cn.chper.avalon.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class UserService {

    private static HashMap<String, String> users = new HashMap<>();

    boolean login(String username, String password) {
        if (!users.containsKey(username)) {
            users.put(username, password);
            return true;
        }
        return users.get(username).equals(password);
    }

}
