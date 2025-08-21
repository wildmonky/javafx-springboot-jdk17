package org.lizhao.validator.exception;

public class PatternNotMatchException extends RuntimeException {

    public PatternNotMatchException() { super(); }

    public PatternNotMatchException(String message) { super(message); }

    public PatternNotMatchException(String message, Throwable cause) { super(message, cause); }

}
