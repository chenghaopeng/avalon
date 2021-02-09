package cn.chper.avalon.service.game;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

import com.alibaba.fastjson.JSONObject;

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

    public void broadcast() {
        JSONObject status = new JSONObject();
        status.put("log", logs);
        status.put("running", state == RoomState.RUNNING);
        for (String username : usernames) {
            SessionController sessionController = SessionController.sessions.get(username);
            if (sessionController == null) continue;
            status.put("info", roleInfo(username));
            String message = Payload.newPayload("status", status);
            Session session = sessionController.session;
            try {
				session.getBasicRemote().sendText(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }

    private String roleInfo(String username) {
        if (state != RoomState.RUNNING) return "";
        StringBuilder sb = new StringBuilder();
        Role role = users.get(username);
        sb.append("你的身份是 ");
        sb.append(role.toString());
        sb.append(" 。\n");
        if (Role.isNormal(role)) sb.append("你的身份不能看到任何人");
        else sb.append("你的身份能看到的是：");
        if (role.type == Role.RedJoker) {
            for (String username2 : users.keySet()) {
                Role role2 = users.get(username2);
                if (role2.type == Role.BlackJoker || role2.type == Role._2 || role2.type == Role.BlackA) {
                    sb.append(" ");
                    sb.append(username2);
                    sb.append(" ");
                }
            }
        }
        if (role.type == Role.King) {
            for (String username2 : users.keySet()) {
                Role role2 = users.get(username2);
                if (role2.type == Role.BlackJoker || role2.type == Role.RedJoker) {
                    sb.append(" ");
                    sb.append(username2);
                    sb.append(" ");
                }
            }
        }
        if (role.type == Role.BlackJoker || role.type == Role._2) {
            for (String username2 : users.keySet()) {
                Role role2 = users.get(username2);
                if (role2.type == Role.BlackJoker || role2.type == Role._2) {
                    sb.append(" ");
                    sb.append(username2);
                    sb.append(" ");
                }
            }
        }
        sb.append("。");
        return sb.toString();
    }

}
