package com.marsmars.util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ApiErrorResponse {
    private String message;
    private Integer status;
    private LocalDateTime errorAt;

    public ApiErrorResponse(String message, LocalDateTime errorAt, Integer status) {
        this.message = message;
        this.status = status;
        this.errorAt = errorAt;
    }
}
