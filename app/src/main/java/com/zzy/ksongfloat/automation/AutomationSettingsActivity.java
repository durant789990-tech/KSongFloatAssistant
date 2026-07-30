package com.zzy.ksongfloat.automation;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zzy.ksongfloat.ui.theme.AppTheme;
import com.zzy.ksongfloat.util.UiKit;

public class AutomationSettingsActivity extends Activity {
    private EditText delayMinSec, delayMaxSec, maxRound, maxTask, failStop, dupFilter, maxDuration;
    private TextView delayMinErr, delayMaxErr;
    private CheckBox commentDraft, pmDraft, autoSendComment, autoSendPm, testMode, pauseLeave, analyzeFirst;
    private LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AutomationSettings s = AutomationSettingsRepository.load(this);
        root = UiKit.root(this);
        root.addView(UiKit.title(this, "自动化设置"));
        root.addView(UiKit.caption(this, "延迟单位为秒。默认开启测试模式，不会真正发送评论或私信。"));

        LinearLayout delayCard = UiKit.card(this);
        delayCard.addView(UiKit.text(this, "随机延迟", AppTheme.BODY_SP, AppTheme.TEXT_PRIMARY, true));
        delayMinSec = numField(String.valueOf(s.delayMinSec()));
        delayMaxSec = numField(String.valueOf(s.delayMaxSec()));
        delayMinErr = errorView();
        delayMaxErr = errorView();
        delayCard.addView(labeled("最小操作延迟（秒）", delayMinSec, delayMinErr));
        delayCard.addView(labeled("最大操作延迟（秒）", delayMaxSec, delayMaxErr));
        root.addView(delayCard);

        LinearLayout limitCard = UiKit.card(this);
        maxRound = numField(String.valueOf(s.maxUsersPerSession));
        maxTask = numField(String.valueOf(s.maxUsersPerTask));
        failStop = numField(String.valueOf(s.consecutiveFailStop));
        dupFilter = numField(String.valueOf(s.duplicateFilterMinutes));
        maxDuration = numField(String.valueOf(s.maxTaskDurationMinutes));
        limitCard.addView(labeled("单轮最大用户数", maxRound, null));
        limitCard.addView(labeled("单次任务最大用户数", maxTask, null));
        limitCard.addView(labeled("连续失败暂停阈值", failStop, null));
        limitCard.addView(labeled("重复用户过滤时间（分钟）", dupFilter, null));
        limitCard.addView(labeled("单次任务最长运行时间（分钟）", maxDuration, null));
        root.addView(limitCard);

        commentDraft = toggle("允许生成评论草稿", s.enableCommentDraft);
        pmDraft = toggle("允许生成私信草稿", s.enablePrivateMessageDraft);
        autoSendComment = toggle("允许自动发送评论（需关闭测试模式）", s.autoSendComment);
        autoSendPm = toggle("允许自动发送私信（需关闭测试模式）", s.autoSendPrivateMessage);
        testMode = toggle("测试模式（只填写不发送）", s.testMode);
        pauseLeave = toggle("离开全民K歌时自动停止", s.pauseOnLeaveKaraoke);
        analyzeFirst = toggle("当前屏无用户时再滑动", s.analyzeBeforeSwipe);

        Button save = UiKit.button(this, "保存设置", AppTheme.BRAND);
        UiKit.attachClick(save, v -> save());
        root.addView(save);
    }

    private EditText numField(String value) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        e.setText(value);
        return e;
    }

    private TextView errorView() {
        TextView t = UiKit.text(this, "", AppTheme.CAPTION_SP, AppTheme.DANGER, false);
        t.setVisibility(TextView.GONE);
        return t;
    }

    private LinearLayout labeled(String label, EditText field, TextView err) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(UiKit.caption(this, label));
        box.addView(field);
        if (err != null) box.addView(err);
        return box;
    }

    private CheckBox toggle(String label, boolean checked) {
        CheckBox c = new CheckBox(this);
        c.setText(label);
        c.setChecked(checked);
        c.setTextColor(AppTheme.TEXT_PRIMARY);
        root.addView(c);
        return c;
    }

    private void save() {
        delayMinErr.setVisibility(TextView.GONE);
        delayMaxErr.setVisibility(TextView.GONE);
        Integer min = parse(delayMinSec, delayMinErr, "最小延迟");
        Integer max = parse(delayMaxSec, delayMaxErr, "最大延迟");
        Integer round = parse(maxRound, null, "单轮人数");
        Integer task = parse(maxTask, null, "任务上限");
        Integer fail = parse(failStop, null, "连续失败次数");
        Integer dup = parse(dupFilter, null, "重复过滤");
        Integer duration = parse(maxDuration, null, "最长运行时间");
        if (min == null || max == null || round == null || task == null || fail == null || dup == null || duration == null) return;
        if (min < 1) { delayMinErr.setText("最小延迟不得小于 1 秒"); delayMinErr.setVisibility(TextView.VISIBLE); return; }
        if (max > 60) { delayMaxErr.setText("最大延迟不得超过 60 秒"); delayMaxErr.setVisibility(TextView.VISIBLE); return; }
        if (min > max) { delayMaxErr.setText("最小延迟不得大于最大延迟"); delayMaxErr.setVisibility(TextView.VISIBLE); return; }
        if (round < 1 || task < 1 || fail < 1) {
            Toast.makeText(this, "人数和失败阈值必须大于 0", Toast.LENGTH_LONG).show();
            return;
        }
        AutomationSettings s = new AutomationSettings();
        s.setDelaySeconds(min, max);
        s.maxUsersPerSession = round;
        s.maxUsersPerTask = task;
        s.consecutiveFailStop = fail;
        s.duplicateFilterMinutes = dup;
        s.maxTaskDurationMinutes = duration;
        s.enableCommentDraft = commentDraft.isChecked();
        s.enablePrivateMessageDraft = pmDraft.isChecked();
        s.autoSendComment = autoSendComment.isChecked();
        s.autoSendPrivateMessage = autoSendPm.isChecked();
        s.autoSend = s.autoSendComment || s.autoSendPrivateMessage;
        s.testMode = testMode.isChecked();
        s.pauseOnLeaveKaraoke = pauseLeave.isChecked();
        s.autoResumeOnReturn = false;
        s.analyzeBeforeSwipe = analyzeFirst.isChecked();
        if ((s.autoSendComment || s.autoSendPrivateMessage) && s.testMode) {
            Toast.makeText(this, "测试模式开启时不能启用自动发送", Toast.LENGTH_LONG).show();
            return;
        }
        AutomationSettingsRepository.save(this, s);
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }

    private Integer parse(EditText e, TextView err, String name) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (Exception ex) {
            if (err != null) {
                err.setText(name + "必须是数字");
                err.setVisibility(TextView.VISIBLE);
            } else {
                Toast.makeText(this, name + "必须是数字", Toast.LENGTH_LONG).show();
            }
            return null;
        }
    }
}
