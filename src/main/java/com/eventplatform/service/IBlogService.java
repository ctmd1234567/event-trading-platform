package com.eventplatform.service;

import com.eventplatform.dto.Result;
import com.eventplatform.entity.Blog;
import com.baomidou.mybatisplus.spring.service.IService;

public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result queryHotBlog(Integer current);

    Result updateLike(Long id);

    Result queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    Result quertBlogOfFollow(Long max, Integer offset);
}
