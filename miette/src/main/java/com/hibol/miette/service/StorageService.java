package com.hibol.miette.service;

import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@ConditionalOnProperty(name = "storage.endpoint")
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3;

    @Value("${storage.bucket}")
    private String bucket;

    public void upload(MultipartFile file, String key) throws IOException {
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            byte[] original = file.getBytes();
            byte[] full  = resize(original, 1920, 0.92);
            byte[] thumb = resize(original,  400, 0.85);
            putBytes(key, full, "image/webp");
            putBytes(thumbKey(key), thumb, "image/webp");
        } else {
            putBytes(key, file.getBytes(), contentType);
        }
    }

    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(thumbKey(key)).build());
        } catch (Exception ignored) {}
    }

    public static String thumbKey(String key) {
        int dot = key.lastIndexOf('.');
        return dot >= 0 ? key.substring(0, dot) + "_thumb" + key.substring(dot) : key + "_thumb";
    }

    private byte[] resize(byte[] input, int maxSize, double quality) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(new java.io.ByteArrayInputStream(input))
                .size(maxSize, maxSize)
                .keepAspectRatio(true)
                .outputQuality(quality)
                .outputFormat("webp")
                .useExifOrientation(true)
                .toOutputStream(out);
        return out.toByteArray();
    }

    private void putBytes(String key, byte[] content, String contentType) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength((long) content.length)
                        .build(),
                RequestBody.fromBytes(content));
    }
}
