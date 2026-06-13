package cn.zest.sso.server.scim;

import cn.zest.sso.common.exception.SsoException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = ScimController.class)
public class ScimExceptionHandler {

    @ExceptionHandler(SsoException.class)
    public ResponseEntity<ScimErrorResponse> handleSsoException(SsoException e) {
        HttpStatus status = switch (e.getCode()) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ScimErrorResponse.builder()
                .schemas(List.of("urn:ietf:params:scim:api:messages:2.0:Error"))
                .status(status.value())
                .detail(e.getMessage())
                .build());
    }
}
