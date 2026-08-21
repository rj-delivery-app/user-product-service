package com.example.userproduct.dto;

import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.io.Serial;
import java.io.Serializable;

@Data
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ImageHolder implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    // Store the binary data and type, which safely serialize in Session State
    private byte[] fileBytes;
    private String originalFileName;
    private String contentType;
}
