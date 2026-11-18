package com.zlf.fufuzhibo.common.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.zlf.fufuzhibo.common.constant.MessageConstant;
import com.zlf.fufuzhibo.common.exception.BaseException;
import com.zlf.fufuzhibo.common.properties.AliOssProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 阿里云 OSS 上传工具
 */
@Slf4j
@Component
public class AliOssUtil {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024L;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Pattern SAFE_DIR = Pattern.compile("[a-zA-Z0-9/_-]+");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-zA-Z0-9]{1,10}");

    private final OSS ossClient;
    private final String bucketName;
    private final String endpointHost;

    public AliOssUtil(AliOssProperties properties) {
        this.bucketName = properties.getBucketName();
        this.endpointHost = normalizeEndpoint(properties.getEndpoint());
        this.ossClient = new OSSClientBuilder().build(
                "https://" + endpointHost,
                properties.getAccessKeyId(),
                properties.getAccessKeySecret());
    }

    /**
     * 通用文件上传
     *
     * @param file 文件
     * @param dir  OSS 目录，例如 avatar、cover、video
     * @return 文件访问 URL
     */
    public String upload(MultipartFile file, String dir) {
        return doUpload(file, dir, null, MAX_FILE_SIZE);
    }

    /**
     * 图片上传，限制图片类型和 5MB
     *
     * @param file 图片文件
     * @param dir  OSS 目录，例如 avatar、cover
     * @return 图片访问 URL
     */
    public String uploadImage(MultipartFile file, String dir) {
        return doUpload(file, dir, "image/", MAX_IMAGE_SIZE);
    }

    private String doUpload(MultipartFile file, String dir, String contentTypePrefix, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(MessageConstant.FILE_EMPTY);
        }
        if (file.getSize() > maxSize) {
            throw new BaseException(contentTypePrefix == null
                    ? MessageConstant.FILE_TOO_LARGE
                    : MessageConstant.IMAGE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        boolean imageOnly = contentTypePrefix != null;
        if (imageOnly && (contentType == null || !contentType.startsWith(contentTypePrefix))) {
            throw new BaseException(MessageConstant.FILE_TYPE_NOT_ALLOWED);
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType, imageOnly);
        String objectKey = normalizeDir(dir) + "/" + LocalDate.now().format(DATE_FORMATTER)
                + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType == null ? "application/octet-stream" : contentType);
        metadata.setContentLength(file.getSize());
        if (contentType == null || !contentType.startsWith("image/")) {
            metadata.setContentDisposition("attachment");
        }

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(bucketName, objectKey, inputStream, metadata);
        } catch (Exception e) {
            log.error("文件上传失败: objectKey={}", objectKey, e);
            throw new BaseException(MessageConstant.FILE_UPLOAD_FAILED);
        }

        return "https://" + bucketName + "." + endpointHost + "/" + objectKey;
    }

    /**
     * 优先按 Content-Type 判断扩展名，无法判断时再回退到原始文件名
     */
    private String resolveExtension(String originalFilename, String contentType, boolean imageOnly) {
        String byContentType = switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> null;
        };
        if (byContentType != null) {
            return byContentType;
        }

        String fileExtension = extensionFromFilename(originalFilename);
        if (fileExtension != null && SAFE_EXTENSION.matcher(fileExtension).matches()) {
            if (imageOnly && !IMAGE_EXTENSIONS.contains(fileExtension)) {
                throw new BaseException(MessageConstant.FILE_TYPE_NOT_ALLOWED);
            }
            return fileExtension;
        }
        if (imageOnly) {
            throw new BaseException(MessageConstant.FILE_TYPE_NOT_ALLOWED);
        }
        return "bin";
    }

    private String extensionFromFilename(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return null;
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeDir(String dir) {
        if (dir == null || dir.isBlank()) {
            return "files";
        }
        String normalized = dir.replace('\\', '/').replaceAll("^/+|/+$", "");
        if (normalized.isEmpty() || normalized.contains("..") || !SAFE_DIR.matcher(normalized).matches()) {
            throw new BaseException(MessageConstant.FILE_DIR_INVALID);
        }
        return normalized;
    }

    private String normalizeEndpoint(String endpoint) {
        return endpoint.replaceFirst("^https?://", "").replaceAll("/+$", "");
    }

    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
