package com.h2football.module.masterdata.application;

public class MasterDataNotFoundException extends RuntimeException {
  public MasterDataNotFoundException(String resource, Object id) {
    super(resource + " not found: " + id);
  }
}
