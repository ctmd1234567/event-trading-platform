package com.eventplatform.service;

import com.eventplatform.dto.Result;
import com.eventplatform.entity.Follow;
import com.baomidou.mybatisplus.spring.service.IService;

public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);
}
