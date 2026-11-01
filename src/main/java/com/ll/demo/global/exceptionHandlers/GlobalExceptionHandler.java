package com.ll.demo.global.exceptionHandlers;

import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.standard.rq.Rq;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.standard.dto.Empty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private final Rq rq;

    // 일반예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resultCode", "500-1");
        body.put("statusCode", 500);
        body.put("msg", ex.getLocalizedMessage());

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        body.put("data", data);

        String path = rq.getCurrentUrlPath();
        data.put("path", path);

        // 스택 트레이스는 응답에 노출하지 않고 서버 로그에만 남김
        log.error("Unhandled exception at {}", path, ex);

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // GlobalException
    @ExceptionHandler(GlobalException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResponseEntity<RsData<Empty>> handleGlobalException(GlobalException ex) {
        RsData<Empty> rsData = ex.getRsData();
        return ResponseEntity
                .status(rsData.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(rsData);
    }

    // 서비스에서 던진 상태 코드 예외
    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public ResponseEntity<RsData<Empty>> handleResponseStatusException(ResponseStatusException ex) {
        return toResponse(ex.getStatusCode(), ex.getReason());
    }

    // Spring MVC 표준 예외 (상태 코드를 스스로 갖는 것들)
    @ExceptionHandler({
            ServletRequestBindingException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class,
            NoResourceFoundException.class,
            MaxUploadSizeExceededException.class,
            HandlerMethodValidationException.class
    })
    @ResponseBody
    public ResponseEntity<RsData<Empty>> handleSpringMvcException(Exception ex) {
        ErrorResponse errorResponse = (ErrorResponse) ex;
        return toResponse(errorResponse.getStatusCode(), errorResponse.getBody().getDetail());
    }

    // 파라미터 타입 오류
    @ExceptionHandler(TypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<RsData<Empty>> handleTypeMismatchException(TypeMismatchException ex) {
        return toResponse(HttpStatus.BAD_REQUEST, "요청 값의 형식이 올바르지 않습니다.");
    }

    // 본문 파싱 오류
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<RsData<Empty>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return toResponse(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다.");
    }

    // Spring Validation 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<RsData<Empty>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        if (fieldError == null) {
            return toResponse(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다.");
        }

        String resultCode = "400-" + fieldError.getCode();
        String msg = fieldError.getField() + " : " + fieldError.getDefaultMessage();

        return handleGlobalException(
                new GlobalException(
                        resultCode,
                        msg
                )
        );
    }

    private ResponseEntity<RsData<Empty>> toResponse(HttpStatusCode status, String msg) {
        return handleGlobalException(new GlobalException(String.valueOf(status.value()), msg));
    }
}
