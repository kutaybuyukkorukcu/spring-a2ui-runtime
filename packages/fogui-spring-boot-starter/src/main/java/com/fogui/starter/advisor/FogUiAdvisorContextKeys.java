package com.fogui.starter.advisor;

/** Shared advisor context keys for cross-module deterministic request tracing. */
public final class FogUiAdvisorContextKeys {

  public static final String REQUEST_ID = "fogui.requestId";
  public static final String ROUTE_MODE = "fogui.routeMode";

  public static final String ROUTE_TRANSFORM = "transform";
  public static final String ROUTE_TRANSFORM_STREAM = "transform-stream";

  private FogUiAdvisorContextKeys() {}
}
