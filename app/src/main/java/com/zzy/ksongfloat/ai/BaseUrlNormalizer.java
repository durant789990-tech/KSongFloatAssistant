package com.zzy.ksongfloat.ai;

import java.net.URI;

public class BaseUrlNormalizer {
    public static class Result {
        public final boolean ok;
        public final String url, warning, error;

        Result(boolean ok, String url, String warning, String error) {
            this.ok = ok;
            this.url = url;
            this.warning = warning;
            this.error = error;
        }
    }

    public static Result normalize(String input) {
        try {
            if (input == null || input.trim().isEmpty()) return new Result(false, "", "", "Base URL 不能为空");
            String s = trimTrailingSlash(input.trim());
            URI u = new URI(s);
            String scheme = u.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return new Result(false, "", "", "必须使用 http 或 https 地址");
            }
            String warn = scheme.equalsIgnoreCase("http") ? "建议使用 HTTPS，避免 API Key 明文传输" : "";
            if (s.endsWith("/v1/chat/completions")) return new Result(true, s, warn, "");
            if (s.endsWith("/chat/completions")) return new Result(true, s, warn, "");
            if (s.endsWith("/v1")) return new Result(true, s + "/chat/completions", warn, "");
            return new Result(true, s + "/v1/chat/completions", warn, "");
        } catch (Exception e) {
            return new Result(false, "", "", "Base URL 格式错误：" + e.getMessage());
        }
    }

    public static Result modelsUrl(String input) {
        try {
            if (input == null || input.trim().isEmpty()) return new Result(false, "", "", "Base URL 不能为空");
            String s = trimTrailingSlash(input.trim());
            URI u = new URI(s);
            String scheme = u.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return new Result(false, "", "", "必须使用 http 或 https 地址");
            }
            String warn = scheme.equalsIgnoreCase("http") ? "建议使用 HTTPS，避免 API Key 明文传输" : "";
            if (s.endsWith("/v1/models")) return new Result(true, s, warn, "");
            if (s.endsWith("/models")) return new Result(true, s, warn, "");
            if (s.endsWith("/v1/chat/completions")) {
                return new Result(true, s.substring(0, s.length() - "/chat/completions".length()) + "/models", warn, "");
            }
            if (s.endsWith("/v1")) return new Result(true, s + "/models", warn, "");
            return new Result(true, s + "/v1/models", warn, "");
        } catch (Exception e) {
            return new Result(false, "", "", "Base URL 格式错误：" + e.getMessage());
        }
    }

    private static String trimTrailingSlash(String s) {
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
