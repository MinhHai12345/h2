package com.h2football.module.match.application;

public class MatchNotFoundException extends RuntimeException {
  public MatchNotFoundException(Long id) {
    super("Match not found: " + id);
  }
}
