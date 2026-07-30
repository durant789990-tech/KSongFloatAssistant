package com.zzy.ksongfloat.ai;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;

import androidx.activity.ComponentActivity;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import com.zzy.ksongfloat.Rounded;
import com.zzy.ksongfloat.feedback.AppFeedbackManager;
import com.zzy.ksongfloat.feedback.HapticFeedbackManager;
import com.zzy.ksongfloat.navigation.AppNavigator;
import com.zzy.ksongfloat.privacy.PrivacySettings;
import com.zzy.ksongfloat.ai.AiConfigRepository;
import com.zzy.ksongfloat.security.SecureStorage;
import com.zzy.ksongfloat.ui.components.*;
import com.zzy.ksongfloat.ui.settings.AiSettingsFormState;
import com.zzy.ksongfloat.ui.settings.AiSettingsValidator;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AiSettingsActivity extends ComponentActivity {
    private LinearLayout root;
    private ScrollView scroll;
    private StatusBanner statusBanner;
    private TextView modelStatus, lastTestStatus, charCount, testHint, modelFetchStatus;
    private EditText base, key, modelManual, timeout, max, customPrompt;
    private MaterialAutoCompleteTextView modelDropdown;
    private TextInputLayout modelInputLayout;
    private Button refreshModelsBtn;
    private ArrayAdapter<String> modelAdapter;
    private final List<String> fetchedModels = new ArrayList<>();
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable modelFetchRunnable;
    private boolean fetchingModels = false;
    private boolean manualModelMode = false;
    private SeekBar temperature;
    private TextView tempValue;
    private Switch strict, ocr, acc, autoDelete, consent;
    private RadioGroup styleGroup;
    private IosPrimaryButton testButton, saveButton;
    private String initialSnapshot = "";
    private boolean keyVisible = false;
    private boolean testing = false;
    private final SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        load();
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { handleBack(true); }
        });
    }

    private void buildUi() {
        scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setBackgroundColor(IosTheme.bg(this));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, statusBarHeight() + dp(10), pad, dp(24));
        scroll.addView(root);
        setContentView(scroll);

        root.setOnClickListener(v -> hideKeyboard());
        IosNavigationBar nav = new IosNavigationBar(this, "AI 接口设置", "保存");
        nav.left.setOnClickListener(v -> handleBack(false));
        nav.right.setOnClickListener(v -> save(false));
        root.addView(nav);

        addStatusCard();
        addInterfaceCard();
        addParamsCard();
        addStyleCard();
        addPrivacyCard();
        addActionCard();
    }

    private void addStatusCard() {
        IosCardView card = new IosCardView(this);
        statusBanner = new StatusBanner(this);
        card.addView(statusBanner);
        modelStatus = small("当前模型：未配置");
        lastTestStatus = small("最近测试：暂无");
        card.addView(modelStatus);
        card.addView(lastTestStatus);
        root.addView(card);
    }

    private void addInterfaceCard() {
        IosCardView card = section("接口信息", "填写你正在使用的 AI 接口信息。API Key 将加密保存在本机。");
        base = input("Base URL", "https://api.example.com/v1", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, false);
        card.addView(labeled("Base URL", base, "示例：https://api.example.com/v1"));
        key = input("API Key", "sk-...", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, false);
        card.addView(labeled("API Key", key, "留空表示不修改已保存的 Key。"));
        LinearLayout keyActions = new LinearLayout(this);
        keyActions.setOrientation(LinearLayout.HORIZONTAL);
        Button show = textAction("显示/隐藏");
        show.setOnClickListener(v -> toggleKey());
        Button clear = textAction("清除 API Key");
        clear.setTextColor(IosTheme.DANGER);
        clear.setOnClickListener(v -> confirmClearKey());
        keyActions.addView(show, new LinearLayout.LayoutParams(0, dp(40), 1));
        keyActions.addView(clear, new LinearLayout.LayoutParams(0, dp(40), 1));
        card.addView(keyActions);
        modelFetchStatus = small("填写 Base URL 和 API Key 后会自动拉取模型列表。");
        card.addView(modelFetchStatus);
        modelInputLayout = new TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle);
        modelInputLayout.setHint("模型名称");
        modelInputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        modelDropdown = new MaterialAutoCompleteTextView(this);
        modelDropdown.setInputType(InputType.TYPE_NULL);
        modelDropdown.setTextSize(15);
        modelDropdown.setDropDownBackgroundDrawable(Rounded.bg(Color.WHITE, dp(12), Color.rgb(220, 224, 232), 1));
        modelDropdown.setDropDownBackgroundDrawable(Rounded.bg(Color.WHITE, dp(12), Color.rgb(220, 224, 232), 1));
        if (android.os.Build.VERSION.SDK_INT >= 21) modelDropdown.setElevation(dp(8));
        modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        modelDropdown.setAdapter(modelAdapter);
        modelDropdown.setOnItemClickListener((parent, view, position, id) -> { markChanged(); updateButtonHint(); });
        modelInputLayout.addView(modelDropdown, new LinearLayout.LayoutParams(-1, -2));
        card.addView(labeledView("模型名称", modelInputLayout, "自动拉取可用模型；失败时可手动输入。"));
        refreshModelsBtn = textAction("刷新模型列表");
        refreshModelsBtn.setOnClickListener(v -> fetchModels(true));
        card.addView(refreshModelsBtn, new LinearLayout.LayoutParams(-1, dp(44)));
        modelManual = input("手动输入模型", "deepseek-chat", InputType.TYPE_CLASS_TEXT, false);
        modelManual.setVisibility(View.GONE);
        card.addView(labeled("手动模型名称", modelManual, "仅在自动拉取失败时使用。"));
        root.addView(card);
    }

    private void addParamsCard() {
        IosCardView card = section("请求参数", "");
        timeout = input("超时时间", "60", InputType.TYPE_CLASS_NUMBER, false);
        card.addView(labeled("超时时间（秒）", timeout, "范围 5–180 秒。"));
        TextView tempLabel = label("Temperature");
        tempValue = small("0.7");
        temperature = new SeekBar(this);
        temperature.setMax(20);
        temperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean f) { tempValue.setText(String.format(Locale.US, "%.1f", p / 10.0)); markChanged(); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        card.addView(tempLabel); card.addView(tempValue); card.addView(temperature);
        max = input("最大输出长度", "1200", InputType.TYPE_CLASS_NUMBER, false);
        card.addView(labeled("最大输出长度（tokens）", max, "范围 100–4000。"));
        strict = switchRow(card, "严格结构化输出", "部分中转站不支持此选项，连接失败时可以关闭后重试。");
        root.addView(card);
    }

    private void addStyleCard() {
        IosCardView card = section("生成风格", "");
        styleGroup = new RadioGroup(this);
        styleGroup.setOrientation(RadioGroup.VERTICAL);
        String[] styles = {"自然友好", "简短直接", "温和礼貌", "幽默轻松", "自定义"};
        for (String s : styles) {
            RadioButton rb = new RadioButton(this);
            rb.setText(s); rb.setTextSize(15); rb.setTextColor(IosTheme.primaryText(this)); rb.setId(s.hashCode());
            styleGroup.addView(rb);
        }
        styleGroup.setOnCheckedChangeListener((g, id) -> markChanged());
        card.addView(styleGroup);
        customPrompt = input("自定义要求", "例如：更克制，不使用表情。", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, true);
        customPrompt.setMinLines(3);
        customPrompt.addTextChangedListener(new SimpleWatcher(){ public void afterTextChanged(Editable e){ charCount.setText(e.length()+"/500"); markChanged(); }});
        card.addView(labeled("自定义要求", customPrompt, ""));
        charCount = small("0/500");
        charCount.setGravity(Gravity.END);
        card.addView(charCount);
        root.addView(card);
    }

    private void addPrivacyCard() {
        IosCardView card = section("隐私", "只发送当前页面公开文字摘要，不上传原始截图或 API Key。");
        ocr = switchRow(card, "允许发送 OCR 文字摘要", "");
        acc = switchRow(card, "允许发送无障碍文字摘要", "");
        autoDelete = switchRow(card, "自动删除分析截图", "");
        consent = switchRow(card, "我确认只发送公开文字摘要", "");
        root.addView(card);
    }

    private void addActionCard() {
        IosCardView card = section("操作", "");
        testButton = new IosPrimaryButton(this, "测试连接", IosTheme.BRAND);
        testButton.setOnClickListener(v -> testConnection());
        card.addView(testButton, new LinearLayout.LayoutParams(-1, dp(52)));
        testHint = small("请先填写 Base URL、API Key 和模型名称。");
        card.addView(testHint);
        saveButton = new IosPrimaryButton(this, "保存设置", IosTheme.BRAND);
        saveButton.setOnClickListener(v -> save(false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52)); lp.setMargins(0, dp(10), 0, 0);
        card.addView(saveButton, lp);
        Button reset = textAction("恢复默认设置");
        reset.setOnClickListener(v -> confirmReset());
        card.addView(reset, new LinearLayout.LayoutParams(-1, dp(44)));
        root.addView(card);
    }

    private void load() {
        AiSettings s = AiSettingsRepository.load(this);
        PrivacySettings p = PrivacySettings.load(this);
        base.setText(s.baseUrl);
        setModelSelection(s.model);
        timeout.setText(String.valueOf(s.timeoutSeconds));
        temperature.setProgress((int)Math.round(s.temperature * 10));
        tempValue.setText(String.format(Locale.US, "%.1f", s.temperature));
        max.setText(String.valueOf(s.maxTokens));
        strict.setChecked(s.strictJson);
        ocr.setChecked(s.allowOcrText);
        acc.setChecked(s.allowAccessibilityText);
        autoDelete.setChecked(p.autoDeleteScreenshots);
        consent.setChecked(s.aiConsent);
        customPrompt.setText(s.customPrompt);
        selectStyle(s.userStyle);
        key.setText("");
        key.setHint(SecureStorage.hasApiKey(this) ? "API Key 已加密保存，留空则不修改" : "API Key");
        updateStatus("未测试", StatusBanner.INFO);
        initialSnapshot = currentState().snapshot();
        attachWatchers();
        updateButtonHint();
        scheduleModelFetch(false);
    }

    private void setModelSelection(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) return;
        if (manualModelMode) {
            modelManual.setText(modelName);
            return;
        }
        if (!fetchedModels.contains(modelName)) {
            fetchedModels.clear();
            fetchedModels.add(modelName);
            modelAdapter.clear();
            modelAdapter.addAll(fetchedModels);
        }
        modelDropdown.setText(modelName, false);
    }

    private String selectedModel() {
        if (manualModelMode) return text(modelManual).trim();
        String v = modelDropdown.getText() == null ? "" : modelDropdown.getText().toString().trim();
        if (!v.isEmpty()) return v;
        return text(modelManual).trim();
    }

    private void scheduleModelFetch(boolean immediate) {
        if (modelFetchRunnable != null) debounceHandler.removeCallbacks(modelFetchRunnable);
        modelFetchRunnable = () -> fetchModels(false);
        debounceHandler.postDelayed(modelFetchRunnable, immediate ? 0 : 1000);
    }

    private void fetchModels(boolean manual) {
        if (fetchingModels) return;
        String baseUrl = text(base).trim();
        String apiKey = text(key).trim();
        try {
            if (apiKey.isEmpty() && SecureStorage.hasApiKey(this)) apiKey = SecureStorage.loadApiKey(this);
        } catch (Exception e) {
            if (manual) fail("读取 API Key 失败：" + e.getMessage());
            return;
        }
        if (baseUrl.isEmpty() || apiKey.isEmpty()) {
            if (manual) fail("请先填写 Base URL 和 API Key");
            return;
        }
        fetchingModels = true;
        refreshModelsBtn.setEnabled(false);
        modelFetchStatus.setText("正在拉取模型列表...");
        updateStatus("正在拉取模型列表...", StatusBanner.INFO);
        int timeoutSec;
        try { timeoutSec = Integer.parseInt(text(timeout)); } catch (Exception e) { timeoutSec = 60; }
        new AiModelFetcher().fetchAsync(baseUrl, apiKey, timeoutSec, result -> runOnUiThread(() -> finishModelFetch(result, manual)));
    }

    private void finishModelFetch(AiModelFetcher.Result result, boolean manual) {
        fetchingModels = false;
        refreshModelsBtn.setEnabled(true);
        if (result.success) {
            manualModelMode = false;
            modelManual.setVisibility(View.GONE);
            fetchedModels.clear();
            fetchedModels.addAll(result.models);
            modelAdapter.clear();
            modelAdapter.addAll(fetchedModels);
            String preferred = selectedModel();
            if (preferred.isEmpty()) preferred = AiSettingsRepository.load(this).model;
            String picked = AiModelFetcher.pickDefaultModel(fetchedModels, preferred.isEmpty() ? "deepseek-chat" : preferred);
            setModelSelection(picked);
            modelFetchStatus.setText("已加载 " + fetchedModels.size() + " 个模型");
            updateStatus("模型列表已更新", StatusBanner.SUCCESS);
            markChanged();
            updateButtonHint();
        } else {
            manualModelMode = true;
            modelManual.setVisibility(View.VISIBLE);
            if (selectedModel().isEmpty() && !AiSettingsRepository.load(this).model.isEmpty()) {
                modelManual.setText(AiSettingsRepository.load(this).model);
            }
            modelFetchStatus.setText("模型获取失败，请检查 URL 和 API Key，可手动输入或点击刷新。");
            if (manual) fail("模型获取失败，请检查 URL 和 API Key\n" + result.error);
            else updateStatus("模型获取失败，请检查 URL 和 API Key", StatusBanner.WARNING);
        }
    }

    private void attachWatchers() {
        TextWatcher watcher = new SimpleWatcher(){ public void afterTextChanged(Editable e){ markChanged(); updateButtonHint(); scheduleModelFetch(false); }};
        TextWatcher baseKeyWatcher = new SimpleWatcher() {
            public void afterTextChanged(Editable e) { markChanged(); updateButtonHint(); scheduleModelFetch(false); }
        };
        base.addTextChangedListener(baseKeyWatcher);
        key.addTextChangedListener(baseKeyWatcher);
        base.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) scheduleModelFetch(false); });
        key.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) scheduleModelFetch(false); });
        modelManual.addTextChangedListener(watcher);
        timeout.addTextChangedListener(watcher); max.addTextChangedListener(watcher);
        strict.setOnCheckedChangeListener((b,c)->markChanged()); ocr.setOnCheckedChangeListener((b,c)->markChanged()); acc.setOnCheckedChangeListener((b,c)->markChanged()); autoDelete.setOnCheckedChangeListener((b,c)->markChanged()); consent.setOnCheckedChangeListener((b,c)->markChanged());
    }

    private AiSettingsFormState currentState() {
        AiSettingsFormState f = new AiSettingsFormState();
        f.baseUrl = text(base); f.apiKeyInput = text(key); f.hasStoredApiKey = SecureStorage.hasApiKey(this);
        f.model = selectedModel(); f.timeout = text(timeout); f.temperature = temperature.getProgress() / 10.0; f.maxTokens = text(max);
        f.strictJson = strict.isChecked(); f.allowOcr = ocr.isChecked(); f.allowAccessibility = acc.isChecked(); f.autoDeleteScreenshots = autoDelete.isChecked(); f.consent = consent.isChecked();
        f.style = selectedStyle(); f.customPrompt = text(customPrompt);
        return f;
    }

    private void save(boolean finishAfter) {
        hideKeyboard();
        AiSettingsFormState f = currentState();
        AiSettingsValidator.Result vr = AiSettingsValidator.validateForSave(f.baseUrl, f.model, f.timeout, f.temperature, f.maxTokens);
        if (!vr.ok) { fail(vr.message); return; }
        try {
            AiSettings s = new AiSettings();
            s.baseUrl = f.baseUrl; s.model = f.model; s.timeoutSeconds = vr.timeoutSeconds; s.temperature = vr.temperature; s.maxTokens = vr.maxTokens;
            s.strictJson = f.strictJson; s.allowOcrText = f.allowOcr; s.allowAccessibilityText = f.allowAccessibility; s.userStyle = f.style; s.customPrompt = f.customPrompt; s.aiConsent = f.consent;
            AiConfigRepository.get().saveConfig(this, s, f.apiKeyInput);
            PrivacySettings ps = PrivacySettings.load(this); ps.autoDeleteScreenshots = f.autoDeleteScreenshots; PrivacySettings.save(this, ps);
            AiSettings verify = AiSettingsRepository.load(this);
            if (!verify.model.equals(s.model)) throw new IllegalStateException("保存后校验失败");
            key.setText("");
            key.setHint(SecureStorage.hasApiKey(this) ? "API Key 已加密保存，留空则不修改" : "API Key");
            initialSnapshot = currentState().snapshot();
            updateStatus("设置已保存", StatusBanner.SUCCESS);
            AppFeedbackManager.success(this, "设置已保存");
            HapticFeedbackManager.confirm(saveButton);
            if (finishAfter) AppNavigator.homeAndFinish(this);
        } catch (Exception e) { fail("保存失败：" + e.getMessage()); }
    }

    private void testConnection() {
        hideKeyboard();
        if (testing) { AppFeedbackManager.warning(this, "正在测试连接，请稍等。"); return; }
        AiSettingsFormState f = currentState();
        AiSettingsValidator.Result vr = AiSettingsValidator.validateForTest(f.baseUrl, f.hasStoredApiKey, f.apiKeyInput, f.model, f.timeout, f.temperature, f.maxTokens);
        if (!vr.ok) { fail(vr.message); return; }
        testing = true;
        testButton.setLoading(true, "正在测试连接……");
        updateStatus("正在测试连接……", StatusBanner.INFO);
        try {
            String api = f.apiKeyInput.trim().length() > 0 ? f.apiKeyInput.trim() : SecureStorage.loadApiKey(this);
            BaseUrlNormalizer.Result n = BaseUrlNormalizer.normalize(f.baseUrl);
            AiSettings s = new AiSettings(); s.baseUrl = f.baseUrl; s.model = f.model; s.timeoutSeconds = vr.timeoutSeconds; s.temperature = vr.temperature; s.maxTokens = Math.min(300, vr.maxTokens); s.strictJson = false;
            JSONObject body = new AiPromptBuilder().buildChatRequest(s, "你是连接测试助手。", "请只回复 OK", false);
            long started = System.currentTimeMillis();
            new AiClient().chatAsync(new AiRequest(n.url, api, body.toString(), vr.timeoutSeconds), new AiClient.Callback() {
                public void onState(AiRequestState st, String msg) { runOnUiThread(() -> updateStatus(msg, StatusBanner.INFO)); }
                public void onResult(AiCallResult r) { runOnUiThread(() -> finishTest(r, f.model, System.currentTimeMillis() - started)); }
            });
        } catch (Exception e) {
            testing = false; testButton.setLoading(false, "测试连接"); fail("测试失败：" + e.getMessage());
        }
    }

    private void finishTest(AiCallResult r, String modelName, long ms) {
        testing = false;
        testButton.setLoading(false, "测试连接");
        if (r.success) {
            updateStatus("连接成功\nHTTP " + r.response.httpCode + "\n模型：" + modelName + "\n耗时：" + String.format(Locale.US, "%.1f 秒", ms / 1000.0) + "\n返回：" + limit(r.response.content, 100), StatusBanner.SUCCESS);
            lastTestStatus.setText("最近测试：成功 · " + fmt.format(new Date()));
            AppFeedbackManager.success(this, "连接成功");
            HapticFeedbackManager.confirm(testButton);
        } else {
            String category = classifyTestError(r);
            fail("连接失败\n结果：" + category + "\nHTTP：" + r.error.httpCode + "\n原因：" + r.error.message + "\n建议：" + r.error.suggestion);
            lastTestStatus.setText("最近测试：" + category + " · " + fmt.format(new Date()));
        }
    }

    private String classifyTestError(AiCallResult r) {
        if (r == null || r.error == null) return "未知错误";
        String type = r.error.type == null ? "" : r.error.type;
        if ("HTTP_401".equals(type) || r.error.httpCode == 401) return "401 Key 错误";
        if ("HTTP_404".equals(type) || r.error.httpCode == 404) return "404 URL 错误";
        if ("TIMEOUT".equals(type) || "HTTP_408".equals(type)) return "超时";
        if ("NETWORK".equals(type) || "DNS".equals(type) || "SSL".equals(type)) return "无网络";
        if ("EMPTY_RESPONSE".equals(type) || type.startsWith("HTTP_4") || type.startsWith("HTTP_5")) return "返回格式错误";
        return type;
    }

    private void handleBack(boolean fromSystemBack) {
        if (fromSystemBack && getCurrentFocus() instanceof EditText) {
            hideKeyboard();
            getCurrentFocus().clearFocus();
            return;
        }
        if (!hasUnsavedChanges()) { AppNavigator.homeAndFinish(this); return; }
        new AlertDialog.Builder(this)
                .setTitle("当前设置尚未保存")
                .setMessage("是否放弃修改？")
                .setNegativeButton("继续编辑", null)
                .setNeutralButton("放弃修改", (d,w) -> AppNavigator.homeAndFinish(this))
                .setPositiveButton("保存并返回", (d,w) -> save(true))
                .show();
    }

    private boolean hasUnsavedChanges() { return !currentState().snapshot().equals(initialSnapshot); }
    private void markChanged() { modelStatus.setText("当前模型：" + (selectedModel().isEmpty() ? "未配置" : selectedModel())); }

    private void updateButtonHint() {
        AiSettingsFormState f = currentState();
        if (f.baseUrl.trim().isEmpty() || f.model.trim().isEmpty() || (!f.hasStoredApiKey && f.apiKeyInput.trim().isEmpty())) testHint.setText("请先填写 Base URL、API Key 并选择模型。");
        else testHint.setText("点击后会发送最小测试请求，不包含 K 歌页面内容。");
    }

    private void updateStatus(String msg, int state) { statusBanner.setState(state, msg); modelStatus.setText("当前模型：" + (selectedModel().isEmpty() ? "未配置" : selectedModel())); }
    private void fail(String msg) { updateStatus(msg, StatusBanner.ERROR); AppFeedbackManager.error(this, msg); HapticFeedbackManager.reject(testButton); }

    private IosCardView section(String title, String subtitle) {
        IosCardView card = new IosCardView(this);
        TextView t = label(title); t.setTextSize(18); card.addView(t);
        if (subtitle != null && subtitle.length() > 0) card.addView(small(subtitle));
        return card;
    }

    private LinearLayout labeled(String label, EditText e, String hint) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, dp(6));
        box.addView(label(label));
        box.addView(e);
        if (hint != null && hint.length() > 0) box.addView(small(hint));
        return box;
    }

    private LinearLayout labeledView(String label, View v, String hint) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(10), 0, dp(6));
        box.addView(label(label));
        box.addView(v);
        if (hint != null && hint.length() > 0) box.addView(small(hint));
        return box;
    }

    private TextView label(String s) { TextView t = new TextView(this); t.setText(s); t.setTextSize(15); t.setTextColor(IosTheme.primaryText(this)); t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); return t; }
    private TextView small(String s) { TextView t = new TextView(this); t.setText(s); t.setTextSize(13); t.setTextColor(IosTheme.secondaryText(this)); t.setPadding(0, dp(4), 0, dp(4)); return t; }
    private EditText input(String label, String hint, int type, boolean multi) { EditText e = new EditText(this); e.setHint(hint); e.setTextSize(15); e.setSingleLine(!multi); e.setInputType(type); e.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE); e.setPadding(dp(12), dp(8), dp(12), dp(8)); e.setBackground(Rounded.bg(IosTheme.bg(this), dp(12), IosTheme.separator(this), 1)); e.setOnEditorActionListener((v, actionId, event) -> { hideKeyboard(); v.clearFocus(); return false; }); return e; }
    private Button textAction(String s) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(IosTheme.BRAND); b.setBackgroundColor(0x00000000); return b; }
    private Switch switchRow(LinearLayout card, String title, String subtitle) { Switch sw = new Switch(this); sw.setText(title + (subtitle == null || subtitle.length() == 0 ? "" : "\n" + subtitle)); sw.setTextSize(15); sw.setTextColor(IosTheme.primaryText(this)); sw.setPadding(0, dp(8), 0, dp(8)); card.addView(sw); return sw; }

    private void selectStyle(String s) {
        String v = s == null ? "" : s;
        String[] styles = {"自然友好", "简短直接", "温和礼貌", "幽默轻松", "自定义"};
        for (String x : styles) if (v.contains(x) || v.equals(x)) { styleGroup.check(x.hashCode()); return; }
        styleGroup.check("自定义".hashCode());
    }
    private String selectedStyle() { int id = styleGroup.getCheckedRadioButtonId(); RadioButton rb = findViewById(id); return rb == null ? "自然友好" : rb.getText().toString(); }
    private void toggleKey() { keyVisible = !keyVisible; key.setInputType(InputType.TYPE_CLASS_TEXT | (keyVisible ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD)); key.setSelection(key.getText().length()); }
    private void confirmClearKey() { new AlertDialog.Builder(this).setTitle("清除 API Key？").setMessage("只会删除本机加密保存的 API Key，不影响其他设置。").setNegativeButton("取消", null).setPositiveButton("清除", (d,w)->{ SecureStorage.clearApiKey(this); AiConfigRepository.get().clearApiKey(this); key.setText(""); key.setHint("API Key"); initialSnapshot = currentState().snapshot(); updateStatus("API Key 已清除", StatusBanner.WARNING); }).show(); }
    private void confirmReset() { new AlertDialog.Builder(this).setTitle("恢复默认设置？").setMessage("不会清除 API Key。如需清除请单独点击“清除 API Key”。").setNegativeButton("取消", null).setPositiveButton("恢复", (d,w)->{ AiSettingsRepository.resetDefaults(this); load(); updateStatus("已恢复默认设置，API Key 未清除", StatusBanner.WARNING); }).show(); }
    private void hideKeyboard() { try { InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE); if(imm!=null&&getCurrentFocus()!=null) imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0); } catch(Exception ignored){} }
    private String text(EditText e) { return e.getText() == null ? "" : e.getText().toString(); }
    private String limit(String s, int n) { return s == null ? "" : (s.length() > n ? s.substring(0, n) + "..." : s); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private int statusBarHeight() { int id = getResources().getIdentifier("status_bar_height", "dimen", "android"); return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24); }

    @Override protected void onDestroy() {
        if (modelFetchRunnable != null) debounceHandler.removeCallbacks(modelFetchRunnable);
        super.onDestroy();
    }

    public static abstract class SimpleWatcher implements TextWatcher { public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){} }
}
