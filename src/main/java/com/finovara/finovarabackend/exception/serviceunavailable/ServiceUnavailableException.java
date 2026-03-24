package com.finovara.finovarabackend.exception.serviceunavailable;

public class ServiceUnavailableException extends RuntimeException {
  public ServiceUnavailableException(String message) {
    super(message);
  }
}
