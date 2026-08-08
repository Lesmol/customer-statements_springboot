package com.lvmp.customerstatements_springboot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StatementFileValidator implements ConstraintValidator<ValidStatementFile, MultipartFile> {
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final byte[] PDF_MAGIC_NUMBER = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final long MAX_FILE_SIZE = DataSize.ofMegabytes(10).toBytes();

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return fail(context, "File must not be empty");
        }

        if (!ALLOWED_CONTENT_TYPE.equals(file.getContentType())) {
            return fail(context, "File must be of type " + ALLOWED_CONTENT_TYPE);
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] head = new byte[PDF_MAGIC_NUMBER.length];
            inputStream.readNBytes(head, 0, head.length);
            if (!Arrays.equals(head, PDF_MAGIC_NUMBER)) {
                return fail(context, "File content does not match a valid PDF");
            }
        } catch (IOException e) {
            return fail(context, "File could not be read");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return fail(context, "File must not exceed 10 MB");
        }

        return true;
    }

    private boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}