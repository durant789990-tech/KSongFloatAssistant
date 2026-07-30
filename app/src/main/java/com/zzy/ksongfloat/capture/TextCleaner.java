package com.zzy.ksongfloat.capture;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TextCleaner {
    public static String cleanLine(String in) {
        if (in == null) return "";
        String s = in.trim().replaceAll("\\s+", " ");
            s = s.replaceAll("[^\\p{IsHan}A-Za-z0-9，。！？、；：（）《》“”\"'@#%&+\\-_/\\[\\]\\s,.!?;:()]", "");
        return s.trim();
    }
    public static List<String> uniqueCleanLines(List<String> lines) {
        Set<String> set = new LinkedHashSet<>();
        if (lines != null) for (String l : lines) { String c = cleanLine(l); if (!c.isEmpty()) set.add(c); }
        return new ArrayList<>(set);
    }
    public static List<String> mergePreferLonger(List<String> a, List<String> b, int max) {
        List<String> all = new ArrayList<>();
        if (a != null) all.addAll(a); if (b != null) all.addAll(b);
        List<String> clean = uniqueCleanLines(all);
        List<String> out = new ArrayList<>();
        outer: for (String s : clean) {
            for (int i=0;i<out.size();i++) {
                String e=out.get(i);
                if (e.contains(s)) continue outer;
                if (s.contains(e)) { out.set(i,s); continue outer; }
            }
            out.add(s); if (out.size()>=max) break;
        }
        return out;
    }
    public static String joinLimit(List<String> lines, int maxChars) {
        StringBuilder sb = new StringBuilder();
        if (lines != null) for (String l: lines) {
            if (l == null || l.isEmpty()) continue;
            if (sb.length() + l.length() + 1 > maxChars) break;
            if (sb.length()>0) sb.append('\n'); sb.append(l);
        }
        return sb.toString();
    }
}
