package com.example.userproduct.entities;

import lombok.Getter;

@Getter
public enum AddressType {
    HOME("home"),
    WORK("work"),
    OTHER("other");

    public final String value;

    AddressType(String value) {
        this.value = value;
    }
}
