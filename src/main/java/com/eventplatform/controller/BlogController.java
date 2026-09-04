package com.eventplatform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eventplatform.dto.Result;
import com.eventplatform.dto.UserDTO;
import com.eventplatform.entity.Blog;
import com.eventplatform.service.IBlogService;
import com.eventplatform.service.IUserService;
import com.eventplatform.utils.SystemConstants;
import com.eventplatform.utils.UserHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;
    @Resource
    private IUserService userService;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
       return blogService.saveBlog(blog);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {

        return blogService.updateLike(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {

        UserDTO user = UserHolder.getUser();

        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));

        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
       return blogService.queryHotBlog(current);
    }

    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long id){
        return blogService.queryBlogById(id);
    }

    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id){
        return blogService.queryBlogLikes(id);
    }

    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current",defaultValue = "1") Integer currrent,
            @RequestParam("id") Long id){

        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(currrent, SystemConstants.MAX_PAGE_SIZE));

        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(
            @RequestParam("lastId") Long max,
            @RequestParam(value = "offset",defaultValue = "0") Integer offset){
        return blogService.quertBlogOfFollow(max,offset);

    }
}
