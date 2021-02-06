package cn.chper.avalon.controller;

import cn.chper.avalon.service.UserService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/login")
    JSONObject login(@RequestBody JSONObject user) {
        JSONObject res = new JSONObject();
        res.put("haha", user.getString("username"));
        res.put("heihei", user.getString("password"));
        return res;
    }

}
