package com.lvmp.apigateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MutableHeaderRequestWrapper extends HttpServletRequestWrapper {
    private final Map<String, String> customHeaders;

    public MutableHeaderRequestWrapper(HttpServletRequest request) {
        super(request);
        this.customHeaders = new ConcurrentHashMap<>();
    }

    public void addHeader(String key, String value) {
        this.customHeaders.put(key, value);
    }

    @Override
    public String getHeader(String name) {
        if (this.customHeaders.containsKey(name)) {
            return this.customHeaders.get(name);
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (this.customHeaders.containsKey(name)) {
            return Collections.enumeration(Collections.singletonList(this.customHeaders.get(name)));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> headerNames = new HashSet<>(this.customHeaders.keySet());

        Enumeration<String> originalHeaders = super.getHeaderNames();

        while (originalHeaders.hasMoreElements()) {
            String headerName = originalHeaders.nextElement();
            headerNames.add(headerName);
        }

        return Collections.enumeration(headerNames);
    }
}
