package com.Backend.Common.Constants;


public final class AppConstants {

    private AppConstants () {

    }

    // Allowed ports for CORS during development
    public static final String FRONTEND_PORT = "3000";
    public static final String BACKEND_PORT = "8001";
    public static final String GATEWAY_PORT = "8000";

    // Allowed origins for CORS in production
    public static final String PRODUCTION_DOMAIN = "your-frontend-domain.com";
    public static final String PRODUCTION_HTTPS = "https://" + PRODUCTION_DOMAIN;
    public static final String PRODUCTION_WS = "wss://" + PRODUCTION_DOMAIN;
    public static final String LOCALHOST_WITH_PORT = "http://localhost:" + FRONTEND_PORT;
}
