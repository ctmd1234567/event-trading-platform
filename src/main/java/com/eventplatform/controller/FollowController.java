package com.eventplatform.controller;

import com.eventplatform.dto.Result;
import com.eventplatform.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId,@PathVariable("isFollow") Boolean isFollow){
        return followService.follow(followUserId,isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result follow(@PathVariable("id") Long followUserId){
        return followService.isFollow(followUserId);
    }

    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable Long id){
        return followService.followCommons(id);
    }
}
