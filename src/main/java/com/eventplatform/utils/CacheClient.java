package com.eventplatform.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PreDestroy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;

@Component
public class CacheClient {
    private final StringRedisTemplate redis;
    private final ExecutorService refresh = new ThreadPoolExecutor(2,4,30,TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(64),new ThreadPoolExecutor.AbortPolicy());
    private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>(
        "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",Long.class);
    public CacheClient(StringRedisTemplate redis) { this.redis=redis; }
    @PreDestroy public void close() { refresh.shutdown(); }
    public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit unit) {
        RedisData data=new RedisData();
        data.setData(value);
        data.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // A physical TTL bounds stale data and reclaims unused entries.
        redis.opsForValue().set(key,JSONUtil.toJsonStr(data),Math.max(60,unit.toSeconds(time)*2),TimeUnit.SECONDS);
    }
    public <R,ID> R queryWithLogicalExpire(String prefix,ID id,Class<R> type,Function<ID,R> load,Long time,TimeUnit unit) {
        String key=prefix+id;
        String cached=redis.opsForValue().get(key);
        if (cached==null) return loadAndCache(key,id,load,time,unit);
        if (cached.isEmpty()) return null;
        RedisData data;
        R value;
        try {
            data=JSONUtil.toBean(cached,RedisData.class);
            value=JSONUtil.toBean(JSONUtil.toJsonStr(data.getData()),type);
            if (data.getExpireTime()==null || value==null) return loadAndCache(key,id,load,time,unit);
        } catch (RuntimeException malformedCache) {
            return loadAndCache(key,id,load,time,unit);
        }
        if(data.getExpireTime().isAfter(LocalDateTime.now())) return value;
        String lockKey=RedisConstants.LOCK_SHOP_KEY+id;
        String owner=UUID.randomUUID().toString();
        if(Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey,owner,10,TimeUnit.SECONDS))) {
            try {
                refresh.execute(() -> {
                    try { loadAndCache(key,id,load,time,unit); }
                    finally { unlock(lockKey,owner); }
                });
            } catch (RejectedExecutionException full) { unlock(lockKey,owner); }
        }
        return value;
    }
    private <R,ID> R loadAndCache(String key,ID id,Function<ID,R> load,Long time,TimeUnit unit) {
        R value=load.apply(id);
        if(value==null) redis.opsForValue().set(key,"",2,TimeUnit.MINUTES);
        else setWithLogicalExpire(key,value,time,unit);
        return value;
    }
    private void unlock(String lockKey,String owner) { redis.execute(UNLOCK,List.of(lockKey),owner); }
}
