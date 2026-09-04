package com.eventplatform;

import com.eventplatform.upload.ImageStorage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;

class ImageStorageTest {
    @TempDir Path temp;
    @Test void reencodesAndRestrictsDeletionToOwner() throws Exception {
        var storage=new ImageStorage(temp.resolve("images").toString());
        var bytes=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes);
        String name=storage.save(1,new MockMultipartFile("file","misleading.html","text/html",bytes.toByteArray()));
        assertThat(Files.exists(storage.resolve(name))).isTrue();
        assertThatThrownBy(() -> storage.delete(2,name)).isInstanceOf(ResponseStatusException.class);
        Path outside=temp.resolve("keep.txt"); Files.writeString(outside,"keep");
        assertThatThrownBy(() -> storage.delete(1,"1/../../keep.txt")).isInstanceOf(ResponseStatusException.class);
        assertThat(Files.readString(outside)).isEqualTo("keep");
        storage.delete(1,name);
        assertThat(Files.exists(temp.resolve("images").resolve(name))).isFalse();
    }
    @Test void rejectsNonImageOversizeAndTraversal() {
        var storage=new ImageStorage(temp.toString());
        assertThatThrownBy(() -> storage.save(1,new MockMultipartFile("file","x.png","image/png","<script>x</script>".getBytes()))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> storage.save(1,new MockMultipartFile("file",new byte[2*1024*1024+1]))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> storage.resolve("../keep.txt")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> storage.resolve("C:\\Windows\\win.ini")).isInstanceOf(ResponseStatusException.class);
    }
}
