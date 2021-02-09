package cn.chper.avalon.controller;

import cn.chper.avalon.form.LoginForm;
import cn.chper.avalon.form.SimpleResponse;
import cn.chper.avalon.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/login")
    SimpleResponse login(@RequestBody LoginForm user) {
        System.out.println("登录：[" + user.getUsername() + "][" + user.getPassword() + "]");
        if (user.getUsername().trim().isEmpty() || user.getPassword().isEmpty()) return new SimpleResponse(false, null);
        if (userService.login(user.getUsername(), user.getPassword())) {
            System.out.println("登录：[" + user.getUsername() + "][" + user.getPassword() + "] 成功");
            return new SimpleResponse(true, userService.getToken(user.getUsername()));
        }
        return new SimpleResponse(false, null);
    }

}
