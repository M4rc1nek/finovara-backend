package com.finovara.finovarabackend.exception;

public class NotAuthorizedException extends RuntimeException {
  public NotAuthorizedException(String message) {
    super(message);
  }
}
