package com.lvmp.customerstatements_springboot.exception;

public class S3UploadException extends RuntimeException {
    public S3UploadException(String message) {
        super(message);
    }

    public S3UploadException(String message, Exception e) {
        super(message, e);
    }
}
