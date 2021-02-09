package cn.chper.avalon.service;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import cn.chper.avalon.form.Payload;
import cn.chper.avalon.service.game.Room;

@Service
public class GameService {
    
    public static ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, Room> userIn = new ConcurrentHashMap<>();

    public static void reenter(String username) {
        if (userIn.containsKey(username)) {
            userIn.get(username).broadcast();
        }
    }

    public static String userOperation(String username, String operation) {
        Payload payload = new Payload(operation);
        switch (payload.type) {
            case "new": {
                String roomNo = RandomStringUtils.randomNumeric(6);
                while (rooms.containsKey(roomNo)) roomNo = RandomStringUtils.randomNumeric(6);
                Room room = new Room();
                rooms.put(roomNo, room);
                return Payload.newPayload("roomno", roomNo);
            }
            case "enter": {
                Room room = rooms.get((String) payload.data);
                if (room == null) {
                    return Payload.newPayload("fail", "房间号错误！");
                }
                if (userIn.get(username) != null) {
                    userIn.get(username).leave(username);
                    userIn.remove(username);
                }
                if (room.enter(username)) {
                    userIn.put(username, room);
                    return null;
                }
                else {
                    return Payload.newPayload("fail", "无法进入房间！");
                }
            }
            case "leave": {
                Room room = userIn.get(username);
                if (room != null) {
                    room.leave(username);
                    userIn.remove(username);
                }
                return null;
            }
            case "start": {
                Room room = userIn.get(username);
                if (room == null) {
                    return Payload.newPayload("fail", "你还未进入房间！");
                }
                if (room.start(username)) {
                    return null;
                }
                else {
                    return Payload.newPayload("fail", "暂时只支持 6 或 7 人局。");
                }
            }
            case "stop": {
                Room room = userIn.get(username);
                if (room == null) {
                    return Payload.newPayload("fail", "你还未进入房间！");
                }
                if (room.stop(username)) {
                    return null;
                }
                else {
                    return Payload.newPayload("fail", "无法结束游戏，问问 pp 是怎么回事。");
                }
            }
            default: return Payload.newPayload("fail", "你发送了奇怪的指令！");
        }
    }

}
