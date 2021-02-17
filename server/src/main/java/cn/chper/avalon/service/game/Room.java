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

    public ConcurrentHashMap<String, String> roleInfos = new ConcurrentHashMap<>();

    public int userCount;

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
        Collections.shuffle(usernames);
        userCount = usernames.size();
        if (userCount <= 5 || userCount >= 9) return false;
        List<Integer> roles = Arrays.asList(Role.roles[userCount]);
        Collections.shuffle(roles);
        users = new ConcurrentHashMap<>();
        for (int i = 0; i < userCount; ++i) {
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
        roleInfos = new ConcurrentHashMap<>();
        generateRoleInfos();
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
        if (array.size() < 2 || array.size() > userCount) return false;
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
            status.put("info", roleInfos.get(username));
            String message = Payload.newPayload("status", status);
            Session session = sessionController.session;
            try {
				session.getBasicRemote().sendText(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }

    private void generateRoleInfos() {
        for (String username : usernames) {
            StringBuilder sb = new StringBuilder();
            Role observer = users.get(username);
            sb.append("你的身份是 ");
            sb.append(observer.toString());
            sb.append(" 。\n");
            sb.append("你的身份能看到的是：");
            sb.append(" ");
            sb.append(username);
            sb.append(" ");
            if (!(observer.type == Role.RedA || (observer.type == Role.BlackA && userCount != 8))) {
                for (String username2 : usernames) {
                    if (username.equals(username2)) continue;
                    Role theObserved = users.get(username2);
                    boolean ruleRedJoker = (observer.type == Role.RedJoker) && (theObserved.type == Role.BlackJoker || theObserved.type == Role.Black2 || theObserved.type == Role.BlackA);
                    boolean ruleRedKing = (observer.type == Role.RedKing) && (theObserved.type == Role.RedJoker || theObserved.type == Role.BlackJoker);
                    boolean ruleBlacks = (observer.type == Role.BlackJoker || observer.type == Role.Black2) && (theObserved.type == Role.BlackJoker || theObserved.type == Role.Black2 || (theObserved.type == Role.BlackA && userCount == 8));
                    boolean ruleBlackA = (observer.type == Role.BlackA) && (userCount == 8) && (theObserved.type == Role.BlackJoker || theObserved.type == Role.Black2);
                    if (ruleRedJoker || ruleRedKing || ruleBlacks || ruleBlackA) {
                        sb.append(" ");
                        sb.append(username2);
                        sb.append(" ");
                    }
                }
            }
            sb.append("。");
            roleInfos.put(username, sb.toString());
        }
    }

}
