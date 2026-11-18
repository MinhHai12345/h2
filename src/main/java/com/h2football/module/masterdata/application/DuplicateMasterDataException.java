package com.h2football.module.masterdata.application;

public class DuplicateMasterDataException extends RuntimeException {
    public DuplicateMasterDataException(String message) { super(message); }
}