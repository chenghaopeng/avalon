package cn.chper.avalon.controller;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.springframework.stereotype.Component;

import cn.chper.avalon.service.GameService;
import cn.chper.avalon.service.UserService;

@Component
@ServerEndpoint("/game/{token}")
public class SessionController {

    public static ConcurrentHashMap<String, SessionController> sessions = new ConcurrentHashMap<>();

    public String username;

    public Session session;

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        String username = UserService.getUsernameByToken(token);
        if (username == null) {
            try {
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        this.username = username;
        this.session = session;
        sessions.put(username, this);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(this.username);
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        String response = GameService.userOperation(this.username, message);
        try {
            if (response != null) session.getBasicRemote().sendText(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
