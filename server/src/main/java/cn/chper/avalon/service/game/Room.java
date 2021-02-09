package cn.chper.avalon.service.game;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.chper.avalon.constant.RoomState;
import cn.chper.avalon.controller.SessionController;
import cn.chper.avalon.form.Payload;

public class Room {

    public String no = "";

    public RoomState state = RoomState.WAITING;

    public ArrayList<String> usernames = new ArrayList<>();

    public ConcurrentHashMap<String, Role> users = new ConcurrentHashMap<>();

    public String logs = "";

    public boolean tasking = false;

    public List<String> taskUsers;

    public List<String> voteUsers;

    public ConcurrentHashMap<String, Boolean> voteOfUsers = new ConcurrentHashMap<>();

    public int failCount = 0;

    public boolean actioning = false;

    public Room(String roomNo, String username) {
        this.no = roomNo;
        this.logs = "房间号 " + roomNo + " ，由 " + username + " 创建\n";
    }

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
        tasking = false;
        taskUsers = new ArrayList<>();
        voteUsers = new ArrayList<>();
        voteOfUsers = new ConcurrentHashMap<>();
        failCount = 0;
        actioning = false;
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

    public boolean task(String username, JSONArray array) {
        if (!usernames.contains(username)) return false;
        if (state != RoomState.RUNNING) return false;
        if (tasking) return false;
        if (array.size() < 2 || array.size() > usernames.size()) return false;
        taskUsers = new ArrayList<>();
        for (int i = 0; i < array.size(); ++i) {
            String username2 = array.getString(i);
            if (!usernames.contains(username2)) {
                return false;
            }
            taskUsers.add(username2);
        }
        voteUsers = new ArrayList<>();
        for (String username2 : usernames) {
            voteUsers.add(username2);
        }
        voteOfUsers = new ConcurrentHashMap<>();
        tasking = true;
        log(username + " 选择了 " + String.join(", ", taskUsers) + " 进行任务，请投票。");
        return true;
    }

    public boolean vote(String username, Boolean choice) {
        if (!usernames.contains(username)) return false;
        if (state != RoomState.RUNNING) return false;
        if (!tasking) return false;
        if (!voteUsers.contains(username)) return false;
        voteUsers.remove(username);
        voteOfUsers.put(username, choice);
        log(username + " 已投票");
        if (voteUsers.size() == 0) {
            StringBuilder sb = new StringBuilder();
            List<String> trues = new ArrayList<>(), falses = new ArrayList<>();
            for (String username2 : voteOfUsers.keySet()) {
                if (voteOfUsers.get(username2)) {
                    trues.add(username2);
                }
                else {
                    falses.add(username2);
                }
            }
            sb.append("投票结束，同意的有 ");
            sb.append(String.join(", ", trues));
            sb.append("，拒绝的有 ");
            sb.append(String.join(", ", falses));
            sb.append(" ，");
            if (trues.size() > falses.size()) {
                sb.append("任务进行。\n");
                sb.append("请 " + String.join(", ", taskUsers) + " 行动。");
                failCount = 0;
                actioning = true;
            }
            else {
                sb.append("任务未能进行。");
                tasking = false;
            }
            log(sb.toString());
        }
        return true;
    }

    public boolean action(String username, Boolean choice) {
        if (!usernames.contains(username)) return false;
        if (state != RoomState.RUNNING) return false;
        if (!tasking) return false;
        if (!actioning) return false;
        if (!taskUsers.contains(username)) return false;
        taskUsers.remove(username);
        if (Role.isBad(users.get(username)) && choice.equals(false)) {
            failCount++;
        }
        log(username + " 已行动");
        if (taskUsers.size() == 0) {
            tasking = false;
            actioning = false;
            log("任务结束，共 " + String.valueOf(failCount) + " 个黑牌，" + (failCount == 0 ? "任务成功" : "任务失败") + "！");
        }
        return true;
    }

    private void log(String message) {
        logs += message + "\n";
        broadcast();
    }

    public void broadcast() {
        JSONObject status = new JSONObject();
        status.put("log", logs);
        status.put("no", no);
        status.put("running", state == RoomState.RUNNING);
        status.put("users", usernames);
        status.put("taskUsers", taskUsers);
        status.put("voteUsers", voteUsers);
        status.put("tasking", tasking);
        status.put("actioning", actioning);
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
