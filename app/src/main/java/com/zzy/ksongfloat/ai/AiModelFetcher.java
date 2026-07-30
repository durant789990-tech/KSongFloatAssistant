package com.zzy.ksongfloat.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AiModelFetcher {
    public static class Result {
        public final boolean success;
        public final List<String> models;
        public final String error;
        public final int httpCode;

        Result(boolean success, List<String> models, String error, int httpCode) {
            this.success = success;
            this.models = models == null ? Collections.emptyList() : models;
            this.error = error == null ? "" : error;
            this.httpCode = httpCode;
        }
    }

    public interface Callback {
        void onResult(Result result);
    }

    public void fetchAsync(String baseUrl, String apiKey, int timeoutSeconds, Callback callback) {
        new Thread(() -> {
            if (callback != null) callback.onResult(fetch(baseUrl, apiKey, timeoutSeconds));
        }, "ai-model-fetch").start();
    }

    public Result fetch(String baseUrl, String apiKey, int timeoutSeconds) {
        BaseUrlNormalizer.Result urlResult = BaseUrlNormalizer.modelsUrl(baseUrl);
        if (!urlResult.ok) return new Result(false, Collections.emptyList(), urlResult.error, 0);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return new Result(false, Collections.emptyList(), "API Key 不能为空", 0);
        }
        int timeout = Math.max(5, timeoutSeconds);
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(timeout, TimeUnit.SECONDS)
                    .readTimeout(timeout, TimeUnit.SECONDS)
                    .writeTimeout(timeout, TimeUnit.SECONDS)
                    .build();
            Request request = new Request.Builder()
                    .url(urlResult.url)
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Accept", "application/json")
                    .get()
                    .build();
            try (Response resp = client.newCall(request).execute()) {
                String body = resp.body() == null ? "" : resp.body().string();
                if (!resp.isSuccessful()) {
                    return new Result(false, Collections.emptyList(), "HTTP " + resp.code() + "：" + limit(body, 180), resp.code());
                }
                return new Result(true, parseModels(body), "", resp.code());
            }
        } catch (Exception e) {
            return new Result(false, Collections.emptyList(), e.getMessage() == null ? "模型拉取失败" : e.getMessage(), 0);
        }
    }

    public static List<String> parseModels(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONArray data = root.optJSONArray("data");
        if (data == null) throw new IllegalStateException("响应缺少 data 字段");
        List<String> out = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = data.optJSONObject(i);
            if (item == null) continue;
            String id = item.optString("id", "").trim();
            if (!id.isEmpty()) out.add(id);
        }
        if (out.isEmpty()) throw new IllegalStateException("未解析到任何模型 id");
        Collections.sort(out, Comparator.naturalOrder());
        return out;
    }

    public static String pickDefaultModel(List<String> models, String preferred) {
        if (models == null || models.isEmpty()) return preferred == null ? "" : preferred;
        if (preferred != null && !preferred.isEmpty()) {
            for (String m : models) if (m.equals(preferred)) return preferred;
            for (String m : models) if (m.contains(preferred)) return m;
        }
        for (String m : models) if ("deepseek-chat".equalsIgnoreCase(m)) return m;
        for (String m : models) if (m.toLowerCase().contains("deepseek-chat")) return m;
        return models.get(0);
    }

    private static String limit(String s, int n) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }
}
