package com.eventplatform.controller;

import com.eventplatform.dto.Result;
import com.eventplatform.upload.ImageStorage;
import com.eventplatform.utils.UserHolder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;

@RestController
@RequestMapping("/upload")
public class UploadController {
    private final ImageStorage images;
    public UploadController(ImageStorage images) { this.images=images; }
    @PostMapping("/blog")
    public Result upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.ok(images.save(UserHolder.getUser().getId(),file));
    }
    @DeleteMapping("/blog")
    public Result delete(@RequestParam("name") String name) throws IOException {
        images.delete(UserHolder.getUser().getId(),name);
        return Result.ok();
    }
    @GetMapping("/images/{user}/{file}")
    public ResponseEntity<Resource> image(@PathVariable String user,@PathVariable String file) throws IOException {
        Path path;
        try { path=images.resolve(user+"/"+file); }
        catch(NoSuchFileException ex) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff")
            .header("Content-Security-Policy","default-src 'none'").body(new FileSystemResource(path));
    }
}
