package com.fogui.service;

/** Stable backend transform and stream error codes. */
public final class TransformErrorCodes {

  public static final String CONTENT_REQUIRED = "CONTENT_REQUIRED";
  public static final String NO_COMPATIBLE_CATALOG = "NO_COMPATIBLE_CATALOG";
  public static final String TRANSFORM_PARSE_FAILED = "TRANSFORM_PARSE_FAILED";
  public static final String A2UI_VALIDATION_FAILED = "A2UI_VALIDATION_FAILED";
  public static final String TRANSFORM_FAILED = "TRANSFORM_FAILED";
  public static final String STREAM_SEND_FAILED = "STREAM_SEND_FAILED";
  public static final String STREAM_FAILED = "STREAM_FAILED";

  private TransformErrorCodes() {}
}
