package com.zzy.ksongfloat.ai;

import com.zzy.ksongfloat.ai.model.*;
import java.util.regex.Pattern;

public class AiSuggestionSafetyFilter {
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)|(?<!\\d)(\\d{3,4}[- ]?\\d{7,8})(?!\\d)");
    private static final Pattern CARD = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");

    public AiSuggestionValidationResult validate(String text) {
        String s = text == null ? "" : text;
        if (s.trim().isEmpty()) return block("内容为空");
        if (PHONE.matcher(s).find()) return block("包含手机号或电话");
        if (CARD.matcher(s).find()) return block("包含疑似银行卡号");
        String l = s.toLowerCase();
        String[] bad = {"微信", "加我", "加微", "私聊", "引流", "合作", "约吗", "裸聊", "转账", "红包",
                "刷礼物", "刷粉", "私奔", "开房", "见面吧", "v信", "qq", "辱骂", "骚扰", "代购", "兼职"};
        for (String b : bad) if (l.contains(b.toLowerCase())) return block("包含风险词：" + b);
        return new AiSuggestionValidationResult(true, "");
    }

    public void filter(AiAnalysisResult r) {
        if (r == null) return;
        for (SuggestionItem it : r.commentSuggestions) mark(it);
        for (SuggestionItem it : r.privateMessageSuggestions) mark(it);
    }

    private void mark(SuggestionItem it) {
        AiSuggestionValidationResult v = validate(it.text);
        it.blocked = !v.allowed;
        it.riskReason = v.reason;
    }

    private AiSuggestionValidationResult block(String r) {
        return new AiSuggestionValidationResult(false, r);
    }
}
