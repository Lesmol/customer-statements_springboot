package com.lvmp.customerstatements_springboot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class StatementFileValidator implements ConstraintValidator<ValidStatementFile, MultipartFile> {
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        return ALLOWED_CONTENT_TYPE.equals(file.getContentType());
    }
}