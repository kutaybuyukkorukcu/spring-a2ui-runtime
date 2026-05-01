package com.fogui.webstarter.service;

import lombok.Getter;

@Getter
public class A2UiActionException extends RuntimeException {

  private final String errorCode;
  private final Object details;

  public A2UiActionException(String message, String errorCode, Object details) {
    super(message);
    this.errorCode = errorCode;
    this.details = details;
  }
}
