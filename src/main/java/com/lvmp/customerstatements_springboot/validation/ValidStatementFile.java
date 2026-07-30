package com.lvmp.customerstatements_springboot.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StatementFileValidator.class)
public @interface ValidStatementFile {
    String message() default "File must be a non-empty PDF";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}