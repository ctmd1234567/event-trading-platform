package com.eventplatform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.eventplatform.dto.LoginFormDTO;
import com.eventplatform.dto.Result;
import com.eventplatform.dto.UserDTO;
import com.eventplatform.entity.User;
import com.eventplatform.mapper.UserMapper;
import com.eventplatform.service.IUserService;
import com.eventplatform.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.eventplatform.utils.RedisConstants.USER_SIGN_KEY;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private com.eventplatform.security.AuthCodes authCodes;

    @Override
    public Result sendCode(String phone) {

        if(RegexUtils.isPhoneInvalid(phone)) {

            return Result.fail("Invalid phone number");
        }

        return authCodes.send(phone);

    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        String code = loginForm.getCode();
        String phone = loginForm.getPhone();

        if(RegexUtils.isPhoneInvalid(phone)) {

            return Result.fail("Invalid phone number");
        }

        if(!authCodes.consume(phone, code)){
            return Result.fail("Invalid verification code");
       }

        User user = query().eq("phone",phone).one();

        if(user==null){
           try {
               user=createUserWithPhone(phone);
           } catch (org.springframework.dao.DuplicateKeyException e) {
               user=query().eq("phone",phone).one();
               if (user == null) throw e;
           }
        }

        String token = UUID.randomUUID().toString(true);

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        String tokenKey=RedisConstants.LOGIN_USER_KEY+token;
        var args = new java.util.ArrayList<String>();
        map.forEach((key,value) -> { args.add(key); args.add(value.toString()); });
        var script = new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
            "redis.call('HSET',KEYS[1],unpack(ARGV)); redis.call('EXPIRE',KEYS[1],1800); return 1", Long.class);
        stringRedisTemplate.execute(script,java.util.List.of(tokenKey),args.toArray());

        return Result.ok(token);
    }

    @Override
    public Result sign() {

        Long userId = UserHolder.getUser().getId();

        LocalDateTime now = LocalDateTime.now();

        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;

        int dayOfMonth = now.getDayOfMonth();

        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {

        Long userId = UserHolder.getUser().getId();

        LocalDateTime now = LocalDateTime.now();

        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;

        int dayOfMonth = now.getDayOfMonth();

        List<Long> result = stringRedisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0));
        if(result==null||result.isEmpty()){

            return Result.ok(0);
        }
        Long num = result.get(0);
        if(num==0||num==null){
            return Result.ok(0);
        }

        int count=0;
        while (true) {

            if((num&1)==0) {

                break;
            }else {

                count++;

                num>>>=1;
            }
        }
        return Result.ok(count);

    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX +RandomUtil.randomString(10));

        save(user);
        return user;

    }
}
