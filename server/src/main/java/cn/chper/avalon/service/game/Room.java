package cn.chper.avalon.service.game;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

import cn.chper.avalon.constant.RoomState;
import cn.chper.avalon.controller.SessionController;
import cn.chper.avalon.form.Payload;

public class Room {

    public RoomState state = RoomState.WAITING;

    public ArrayList<String> usernames = new ArrayList<>();

    public ConcurrentHashMap<String, Role> users = new ConcurrentHashMap<>();

    public String logs = "";

    public boolean enter(String username) {
        if (state != RoomState.WAITING) return false;
        if (usernames.contains(username)) return false;
        usernames.add(username);
        log(username + " 进入了房间");
        return true;
    }

    public boolean leave(String username) {
        if (!usernames.contains(username)) return false;
        stop(username);
        usernames.remove(username);
        log(username + " 离开了房间");
        return true;
    }

    public boolean start(String starterUsername) {
        if (state != RoomState.WAITING) return false;
        if (!usernames.contains(starterUsername)) return false;
        if (usernames.size() <= 5 || usernames.size() >= 8) return false;
        List<Integer> roles = Arrays.asList(Role.roles[usernames.size()]);
        Collections.shuffle(roles);
        users = new ConcurrentHashMap<>();
        for (int i = 0; i < usernames.size(); ++i) {
            String username = usernames.get(i);
            Integer role = roles.get(i);
            users.put(username, new Role(role));
        }
        state = RoomState.RUNNING;
        log(starterUsername + " 开始了游戏");
        // TODO: 给不同角色发不同的“看到”信息
        return true;
    }

    public boolean stop(String stoperUsername) {
        if (!usernames.contains(stoperUsername)) return false;
        if (state == RoomState.WAITING) return true;
        state = RoomState.WAITING;
        log(stoperUsername + " 结束了游戏");
        return true;
    }

    private void log(String message) {
        logs += message + "\n";
        broadcast();
    }

    private void broadcast() {
        String message = Payload.newPayload("log", logs);
        for (String username : usernames) {
            SessionController sessionController = SessionController.sessions.get(username);
            if (sessionController == null) continue;
            Session session = sessionController.session;
            try {
				session.getBasicRemote().sendText(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }

}
