package login.tikichat.global.component.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import login.tikichat.global.component.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {
    private final AmazonS3 amazonS3;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Override
    public void upload(String path, InputStream file) {
        amazonS3.putObject(bucketName, path, file, new ObjectMetadata());
    }

    @Override
    public String getUrl(String path) {
        return amazonS3.getUrl(bucketName, path).toString();
    }
}
