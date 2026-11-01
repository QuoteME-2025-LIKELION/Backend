package com.ll.demo.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Year;

// 4자리 숫자인 출생년도가 1900~현재 연도인지 검증 (형식은 @Pattern이 담당)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BirthYearRange.Validator.class)
public @interface BirthYearRange {
    String message() default "출생년도는 1900년부터 현재 연도까지 입력할 수 있습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<BirthYearRange, String> {
        private static final int MIN_YEAR = 1900;

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || !value.matches("\\d{4}")) {
                return true;
            }
            int year = Integer.parseInt(value);
            return year >= MIN_YEAR && year <= Year.now().getValue();
        }
    }
}
