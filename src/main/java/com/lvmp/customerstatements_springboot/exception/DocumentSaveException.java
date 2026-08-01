package com.lvmp.customerstatements_springboot.exception;

public class DocumentSaveException extends RuntimeException {
    public DocumentSaveException(String message) {
        super(message);
    }
}
