package com.eventplatform.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

@Service
public class ImageStorage {
    private final Path base;
    public ImageStorage(@Value("${app.upload.directory:./data/images}") String directory) {
        this.base=Path.of(directory).toAbsolutePath().normalize();
    }
    public String save(long user,MultipartFile upload) throws IOException {
        if(upload.isEmpty() || upload.getSize()>2*1024*1024) throw invalid();
        BufferedImage image;
        // Inspect dimensions before decoding; restrict formats and re-encode to remove metadata and appended payloads.
        try(var input=ImageIO.createImageInputStream(upload.getInputStream())) {
            var readers=ImageIO.getImageReaders(input);
            if(!readers.hasNext()) throw invalid();
            var reader=readers.next();
            try {
                String format=reader.getFormatName();
                if(!format.equalsIgnoreCase("PNG") && !format.equalsIgnoreCase("JPEG")) throw invalid();
                reader.setInput(input);
                long pixels=(long)reader.getWidth(0)*reader.getHeight(0);
                if(pixels<=0 || pixels>16_000_000) throw invalid();
                image=reader.read(0);
            } finally { reader.dispose(); }
        }
        Files.createDirectories(base);
        Path root=base.toRealPath();
        Path userDir=root.resolve(Long.toString(user));
        if(!Files.exists(userDir,LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(userDir);
        if(Files.isSymbolicLink(userDir) || !userDir.toRealPath().getParent().equals(root)) throw invalid();
        String relative=user+"/"+UUID.randomUUID()+".png";
        Path file=root.resolve(relative);
        OutputStream created=Files.newOutputStream(file,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);
        try(OutputStream output=created) {
            if(!ImageIO.write(image,"png",output)) throw invalid();
        } catch(Exception e) { Files.deleteIfExists(file); throw e; }
        return relative;
    }
    public Path resolve(String name) throws IOException {
        if(name==null || !name.matches("[1-9][0-9]*/[a-f0-9-]{36}\\.png")) throw invalid();
        Path root=base.toRealPath();
        Path path=root.resolve(name).normalize();
        if(!path.startsWith(root) || Files.isSymbolicLink(path.getParent()) || Files.isSymbolicLink(path)
            || !path.getParent().toRealPath().getParent().equals(root)) throw invalid();
        return path;
    }
    public void delete(long user,String name) throws IOException {
        if(name==null || !name.startsWith(user+"/")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can only delete images you uploaded");
        Path path=resolve(name);
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");
        Files.delete(path);
    }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid image or file path"); }
}
