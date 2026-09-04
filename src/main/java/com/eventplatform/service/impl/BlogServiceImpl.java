package com.eventplatform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eventplatform.dto.Result;
import com.eventplatform.dto.ScrollResult;
import com.eventplatform.dto.UserDTO;
import com.eventplatform.entity.Blog;
import com.eventplatform.entity.Follow;
import com.eventplatform.entity.User;
import com.eventplatform.mapper.BlogMapper;
import com.eventplatform.service.IBlogService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.eventplatform.service.IFollowService;
import com.eventplatform.service.IUserService;
import com.eventplatform.utils.SystemConstants;
import com.eventplatform.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.eventplatform.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.eventplatform.utils.RedisConstants.FEED_KEY;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private IFollowService followService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryHotBlog(Integer current) {

        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        List<Blog> records = page.getRecords();

        records.forEach(blog -> {
            this.isBlogLiked(blog);
            this.queryBlogUser(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result updateLike(Long id){

        Long userId = UserHolder.getUser().getId();

        String key=BLOG_LIKED_KEY+id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(score==null) {

            boolean isSuccess = update().setSql("liked=liked+1").eq("id", id).update();

            if(isSuccess){
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }else {

            boolean isSuccess = update().setSql("liked=liked-1").eq("id", id).update();
            if(isSuccess) {

                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {

        String key=BLOG_LIKED_KEY+id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if(top5==null||top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",",ids);

        List<UserDTO> userDTOS = userService.query()
                .in("id",ids).last("order by field(id,"+idStr+")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());

        return Result.ok(userDTOS);

    }

    @Override
    public Result saveBlog(Blog blog) {

        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());

        boolean isSuccess = save(blog);
        if(!isSuccess){
            return Result.fail("Failed to create post");
        }

        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();

        for (Follow follow : follows) {

            Long userId = follow.getUserId();

            String key=FEED_KEY+userId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }

        return Result.ok(blog.getId());
    }

    @Override
    public Result quertBlogOfFollow(Long max, Integer offset) {

        Long userId = UserHolder.getUser().getId();

        String key=FEED_KEY+userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        if(typedTuples==null||typedTuples.isEmpty()){
            return Result.ok();
        }

        List<Long> ids=new ArrayList<>(typedTuples.size());
        long minTime=0;
        int os=1;
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {

            ids.add(Long.valueOf( typedTuple.getValue()));

            long time=typedTuple.getScore().longValue();
            if(time==minTime){
                os++;
            }else {
                minTime = time;
                os = 1;
            }

        }

        String idStr = StrUtil.join(",", ids);
        List<Blog> blogs = query()
                .in("id", ids).last("order by field(id," + idStr + ")").list();

        for (Blog blog : blogs) {

            queryBlogUser(blog);

            isBlogLiked(blog);
        }

        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);

    }

    @Override
    public Result  queryBlogById(Long id) {

        Blog blog = getById(id);

        if(blog==null){
            return Result.fail("Post not found");
        }
        queryBlogUser(blog);

        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {

        UserDTO user = UserHolder.getUser();
        if(user==null){

            return;
        }
        Long userId = user.getId();

        String key=BLOG_LIKED_KEY+blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score!=null);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

}
