package login.tikichat.global.component;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorage {
    void upload(String path, InputStream InputStream) throws IOException;
    String getUrl(String path);
}
