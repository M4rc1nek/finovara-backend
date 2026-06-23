package com.finovara.authservice.exception.conflict;

public class LocalPasswordNotSetException extends RuntimeException {
  public LocalPasswordNotSetException(String message) {
    super(message);
  }
}
