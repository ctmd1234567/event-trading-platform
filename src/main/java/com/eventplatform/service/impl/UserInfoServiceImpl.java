package com.eventplatform.service.impl;

import com.eventplatform.entity.UserInfo;
import com.eventplatform.mapper.UserInfoMapper;
import com.eventplatform.service.IUserInfoService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
