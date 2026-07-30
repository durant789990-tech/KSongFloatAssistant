package com.zzy.ksongfloat.automation;

import com.zzy.ksongfloat.capture.PageTextResult;
import com.zzy.ksongfloat.classifier.PageClassificationResult;

import java.util.ArrayList;
import java.util.List;

public class UserProfileInfo {
    public String nickname = "";
    public String bio = "";
    public List<String> songTitles = new ArrayList<>();
    public String mergedText = "";

    public static UserProfileInfo from(PageTextResult page, PageClassificationResult cls) {
        UserProfileInfo info = new UserProfileInfo();
        if (page != null) info.mergedText = page.mergedText == null ? "" : page.mergedText;
        if (cls != null) {
            info.nickname = cls.detectedNickname == null ? "" : cls.detectedNickname;
            if (cls.detectedSongTitles != null) info.songTitles.addAll(cls.detectedSongTitles);
        }
        if (info.bio.isEmpty() && page != null && page.mergedText != null) {
            for (String line : page.mergedText.split("\\n")) {
                if (line.length() > 8 && line.length() < 80 && !line.contains("粉丝") && !line.contains("关注")) {
                    info.bio = line;
                    break;
                }
            }
        }
        if (info.nickname.isEmpty() && page != null && page.mergedText != null) {
            for (String line : page.mergedText.split("\\n")) {
                if (line.length() > 1 && line.length() < 18) {
                    info.nickname = line;
                    break;
                }
            }
        }
        return info;
    }
}
