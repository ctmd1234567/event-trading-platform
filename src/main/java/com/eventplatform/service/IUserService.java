package com.eventplatform.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.eventplatform.dto.LoginFormDTO;
import com.eventplatform.dto.Result;
import com.eventplatform.entity.User;

public interface IUserService extends IService<User> {

    Result sendCode(String phone);

    Result login(LoginFormDTO loginForm);

    Result sign();

    Result signCount();
}
