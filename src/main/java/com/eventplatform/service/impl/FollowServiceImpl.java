package com.eventplatform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.eventplatform.dto.Result;
import com.eventplatform.dto.UserDTO;
import com.eventplatform.entity.Follow;
import com.eventplatform.mapper.FollowMapper;
import com.eventplatform.service.IFollowService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.eventplatform.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserServiceImpl userService;
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {

        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;

        if(isFollow) {

            Follow follow = new Follow();
            follow.setFollowUserId(followUserId);
            follow.setUserId(userId);
            boolean isSuccess = save(follow);
            if(isSuccess){

                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }
        }else {

            boolean isSuccess = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));

            if(isSuccess){
                stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();

        long count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
            return Result.ok(count>0);

    }

    @Override
    public Result followCommons(Long id) {

        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;

        String key2 = "follows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if(intersect==null||intersect.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        List<UserDTO> userDTOS = userService
                .listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);

    }
}
