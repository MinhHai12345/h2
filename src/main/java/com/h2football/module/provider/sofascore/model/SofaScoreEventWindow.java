package com.h2football.module.provider.sofascore.model;

public enum SofaScoreEventWindow {
  LAST("last"),
  NEXT("next");

  private final String pathSegment;

  SofaScoreEventWindow(String pathSegment) {
    this.pathSegment = pathSegment;
  }

  public String pathSegment() {
    return pathSegment;
  }
}
