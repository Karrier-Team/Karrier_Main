package com.karrier.mentoring.http.error.exception;

import com.karrier.mentoring.http.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UnsupportedMediaTypeException extends RuntimeException{
    private final ErrorCode errorCode;
}
