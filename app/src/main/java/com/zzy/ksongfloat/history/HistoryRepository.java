package com.zzy.ksongfloat.history;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryRepository {
    public interface Callback<T> { void onResult(T value); void onError(Exception e); }
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    public static void all(Context c, Callback<List<InteractionRecord>> cb) {
        IO.execute(() -> {
            try { cb.onResult(AppDatabase.get(c).interactionRecordDao().all()); }
            catch (Exception e) { cb.onError(e); }
        });
    }

    public static void byStatus(Context c, String status, Callback<List<InteractionRecord>> cb) {
        IO.execute(() -> {
            try { cb.onResult(AppDatabase.get(c).interactionRecordDao().byStatus(status)); }
            catch (Exception e) { cb.onError(e); }
        });
    }

    public static void search(Context c, String keyword, Callback<List<InteractionRecord>> cb) {
        IO.execute(() -> {
            try { cb.onResult(AppDatabase.get(c).interactionRecordDao().searchNickname(keyword == null ? "" : keyword)); }
            catch (Exception e) { cb.onError(e); }
        });
    }

    public static void upsert(Context c, InteractionRecord record, Callback<InteractionRecord> cb) {
        IO.execute(() -> {
            try {
                InteractionRecordDao dao = AppDatabase.get(c).interactionRecordDao();
                long now = System.currentTimeMillis();
                InteractionRecord old = dao.byFingerprint(record.profileFingerprint);
                if (old == null) {
                    if (record.firstSeenAt == 0) record.firstSeenAt = now;
                    record.lastSeenAt = now;
                    record.id = dao.insert(record);
                    cb.onResult(record);
                } else {
                    old.nickname = value(record.nickname, old.nickname);
                    old.visibleBio = value(record.visibleBio, old.visibleBio);
                    old.visibleSongs = value(record.visibleSongs, old.visibleSongs);
                    old.pageType = value(record.pageType, old.pageType);
                    old.lastSeenAt = now;
                    if (record.lastAnalyzedAt > 0) old.lastAnalyzedAt = record.lastAnalyzedAt;
                    old.generatedComments = value(record.generatedComments, old.generatedComments);
                    old.generatedMessages = value(record.generatedMessages, old.generatedMessages);
                    old.riskFlags = value(record.riskFlags, old.riskFlags);
                    old.detectedLocationText = value(record.detectedLocationText, old.detectedLocationText);
                    if (record.interactionStatus != null && record.interactionStatus.length() > 0) old.interactionStatus = record.interactionStatus;
                    dao.update(old);
                    cb.onResult(old);
                }
            } catch (Exception e) { cb.onError(e); }
        });
    }

    public static void updateStatus(Context c, long id, String status, Callback<Boolean> cb) {
        IO.execute(() -> {
            try { AppDatabase.get(c).interactionRecordDao().updateStatus(id, status, System.currentTimeMillis()); cb.onResult(true); }
            catch (Exception e) { cb.onError(e); }
        });
    }

    public static void updateNotes(Context c, long id, String notes, Callback<Boolean> cb) {
        IO.execute(() -> {
            try { AppDatabase.get(c).interactionRecordDao().updateNotes(id, notes == null ? "" : notes, System.currentTimeMillis()); cb.onResult(true); }
            catch (Exception e) { cb.onError(e); }
        });
    }

    public static void delete(Context c, InteractionRecord r, Callback<Boolean> cb) {
        IO.execute(() -> {
            try { AppDatabase.get(c).interactionRecordDao().delete(r); cb.onResult(true); }
            catch (Exception e) { cb.onError(e); }
        });
    }

    public static void clear(Context c, Callback<Boolean> cb) {
        IO.execute(() -> {
            try { AppDatabase.get(c).interactionRecordDao().clearAll(); cb.onResult(true); }
            catch (Exception e) { cb.onError(e); }
        });
    }

    public static void exportJson(Context c, Callback<String> cb) {
        IO.execute(() -> {
            try {
                JSONArray arr = new JSONArray();
                for (InteractionRecord r : AppDatabase.get(c).interactionRecordDao().all()) arr.put(toJson(r));
                cb.onResult(arr.toString(2));
            } catch (Exception e) { cb.onError(e); }
        });
    }

    public static JSONObject toJson(InteractionRecord r) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", r.id);
        o.put("profileFingerprint", r.profileFingerprint);
        o.put("nickname", r.nickname);
        o.put("visibleBio", r.visibleBio);
        o.put("visibleSongs", r.visibleSongs);
        o.put("pageType", r.pageType);
        o.put("firstSeenAt", r.firstSeenAt);
        o.put("lastSeenAt", r.lastSeenAt);
        o.put("lastAnalyzedAt", r.lastAnalyzedAt);
        o.put("generatedComments", new JSONArray(r.generatedComments == null || r.generatedComments.length() == 0 ? "[]" : r.generatedComments));
        o.put("generatedMessages", new JSONArray(r.generatedMessages == null || r.generatedMessages.length() == 0 ? "[]" : r.generatedMessages));
        o.put("interactionStatus", r.interactionStatus);
        o.put("userNotes", r.userNotes);
        o.put("detectedLocationText", r.detectedLocationText);
        o.put("riskFlags", new JSONArray(r.riskFlags == null || r.riskFlags.length() == 0 ? "[]" : r.riskFlags));
        return o;
    }

    private static String value(String n, String old) {
        return n == null || n.length() == 0 ? (old == null ? "" : old) : n;
    }
}
