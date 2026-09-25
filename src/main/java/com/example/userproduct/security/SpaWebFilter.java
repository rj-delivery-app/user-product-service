package com.example.userproduct.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.regex.Pattern;

@Configuration
public class SpaWebFilter implements Filter {

    // Regex to strictly match static file extensions (.js, .css, .png, .jpg, .ico, .svg, .json)
    private static final Pattern STATIC_FILE_PATTERN =
            Pattern.compile(".*\\.(js|css|png|jpg|jpeg|gif|ico|svg|json|woff|woff2|ttf)$", Pattern.CASE_INSENSITIVE);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();

        // 1. Skip backend API endpoints so they process normally
        // 2. Skip static assets like JS, CSS, images, and JSON configs
        if (path.startsWith("/api/") ||
                path.startsWith("/assets/") ||
                path.startsWith("/files/") ||
                path.equals("/favicon.ico") ||
                path.equals("/favicon.ico") ||
                path.startsWith("/actuator") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/") ||
                STATIC_FILE_PATTERN.matcher(path).matches()) {

            chain.doFilter(request, response);
            return;
        }

        // 3. For any other frontend routes (like /merchant/products), forward to React's index.html
        req.getRequestDispatcher("/index.html").forward(request, response);
    }
}
