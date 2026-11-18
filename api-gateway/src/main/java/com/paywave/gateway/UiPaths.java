package com.paywave.gateway;

final class UiPaths {

    private UiPaths() {
    }

    static boolean isPublicGet(String path) {
        if (path == null) {
            return false;
        }
        return path.equals("/")
                || path.equals("/index.html")
                || path.startsWith("/css/")
                || path.startsWith("/js/");
    }

    static String classpathResource(String path) {
        if (path.equals("/")) {
            return "classpath:static/index.html";
        }
        return "classpath:static" + path;
    }
}