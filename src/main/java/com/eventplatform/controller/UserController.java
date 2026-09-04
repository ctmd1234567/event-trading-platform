package com.eventplatform.controller;

import cn.hutool.core.bean.BeanUtil;
import com.eventplatform.dto.LoginFormDTO;
import com.eventplatform.dto.Result;
import com.eventplatform.dto.UserDTO;
import com.eventplatform.entity.User;
import com.eventplatform.entity.UserInfo;
import com.eventplatform.service.IUserInfoService;
import com.eventplatform.service.IUserService;
import com.eventplatform.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private com.eventplatform.security.RequestLimits limits;

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, jakarta.servlet.http.HttpServletRequest request) {
        limits.auth(request);
        return userService.sendCode(phone);
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, jakarta.servlet.http.HttpServletRequest request){
        limits.auth(request);
        return userService.login(loginForm);
    }

    @PostMapping("/logout")
    public Result logout(jakarta.servlet.http.HttpServletRequest request){
        String token = com.eventplatform.security.TokenFilter.token(request);
        if (token != null) redisTemplate.delete(com.eventplatform.utils.RedisConstants.LOGIN_USER_KEY + token);
        UserHolder.removeUser();
        return Result.ok();
    }

    @GetMapping("/me")
    public Result me(){
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){

        UserInfo info = userInfoService.getById(userId);
        if (info == null) {

            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);

        return Result.ok(info);
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){

        User user = userService.getById(userId);
        if(user==null){
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        return Result.ok(userDTO);
    }

    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}
