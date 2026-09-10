package org.angelauramc.methodsInjectorAgent.minethor;

final class JsonProtocol {
    private JsonProtocol() {
    }

    static int intValue(String json, String key, int fallback) {
        String value = rawValue(json, key);
        if (value == null) return fallback;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static String stringValue(String json, String key, String fallback) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return fallback;

        start += marker.length();
        int end = json.indexOf('"', start);
        if (end < 0) return fallback;
        return json.substring(start, end);
    }

    private static String rawValue(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return null;

        start += marker.length();
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if ((c < '0' || c > '9') && c != '-') break;
            end++;
        }
        if (end == start) return null;
        return json.substring(start, end);
    }
}
