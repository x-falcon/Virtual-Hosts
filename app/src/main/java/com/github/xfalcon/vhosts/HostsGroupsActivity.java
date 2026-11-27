package com.github.xfalcon.vhosts;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.Gravity;
import java.util.HashMap;
import java.util.Map;
import androidx.core.content.ContextCompat;
import android.net.Uri;
import android.os.Bundle;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import com.github.xfalcon.vhosts.groups.HostsGroupStore;
import com.github.xfalcon.vhosts.groups.HostsGroups;
import com.github.xfalcon.vhosts.vservice.DnsChange;
import com.github.xfalcon.vhosts.vservice.VhostsService;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

/** Owns imported copies; deleting/moving a source document does not break saved groups. */
public class HostsGroupsActivity extends AppCompatActivity {
    private static final int IMPORT = 71;
    private HostsGroupStore store;
    private List<HostsGroups.Group> groups = new ArrayList<>();
    private LinearLayout rows;
    private TextView status;
    private Button importButton;
    private Button newButton;
    private final Map<String, String> drafts = new HashMap<>();
    private EditText creatingName, creatingContent;
    private String restoredName = "", restoredContent = "";
    private boolean creating;
    private boolean importing;
    private final Set<String> expandedGroups = new HashSet<>();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Hosts 分组");
        if (savedInstanceState != null) {
            ArrayList<String> expanded = savedInstanceState.getStringArrayList("expanded_groups");
            if (expanded != null) expandedGroups.addAll(expanded);
            if (savedInstanceState.getBoolean("editor_state_saved")) restoreEditorState();
        }
        store = new HostsGroupStore(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        page.setPadding(pad, pad, pad, pad);
        status = new TextView(this);
        page.addView(status);
        importButton = new Button(this);
        importButton.setText("导入 hosts 文件（可多选）");
        page.addView(importButton);
        newButton = new Button(this);
        newButton.setText("手动新建 hosts 分组");
        newButton.setOnClickListener(v -> createGroup());
        page.addView(newButton);
        ScrollView scroll = new ScrollView(this);
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(page);
        com.github.xfalcon.vhosts.util.SystemBarInsets.apply(this);
        try {
            List<HostsGroups.Group> saved = store.load();
            if (saved != null) groups = saved;
            else {
                try { migrateLegacy(); }
                catch (Exception e) {
                    groups.clear();
                    new AlertDialog.Builder(this).setTitle("原有 hosts 未迁移")
                            .setMessage("旧文件无法读取或解析。原文件和设置保留，你仍可导入新分组。\n" + e.getMessage())
                            .setPositiveButton("确定", null).show();
                }
            }
        } catch (Exception e) {
            showError(e);
            importButton.setEnabled(false);
            newButton.setEnabled(false);
            status.setText("读取分组失败；原配置未修改，请返回检查。");
            return;
        }
        importButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
                    .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            try { startActivityForResult(intent, IMPORT); }
            catch (Exception e) { showError(e); }
        });
        render();
        if (creating) createGroup();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList("expanded_groups", new ArrayList<>(expandedGroups));
        // Hosts can be 2 MB; keep text off Binder's small saved-state transaction.
        AtomicFile file = editorStateFile();
        FileOutputStream stream = null;
        try {
            JSONObject state = new JSONObject().put("drafts", new JSONObject(drafts)).put("creating", creating);
            if (creating && creatingName != null) state.put("name", creatingName.getText().toString())
                    .put("content", creatingContent.getText().toString());
            stream = file.startWrite();
            stream.write(state.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(stream);
            outState.putBoolean("editor_state_saved", true);
        } catch (Exception e) {
            if (stream != null) file.failWrite(stream);
            Toast.makeText(this, "草稿保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        super.onSaveInstanceState(outState);
    }

    private AtomicFile editorStateFile() {
        return new AtomicFile(new File(getFilesDir(), "hosts-editor-state.json"));
    }

    private void restoreEditorState() {
        try {
            JSONObject state = new JSONObject(new String(editorStateFile().readFully(), StandardCharsets.UTF_8));
            JSONObject saved = state.getJSONObject("drafts");
            java.util.Iterator<String> keys = saved.keys();
            while (keys.hasNext()) {
                String id = keys.next();
                drafts.put(id, saved.getString(id));
            }
            creating = state.optBoolean("creating");
            restoredName = state.optString("name");
            restoredContent = state.optString("content");
        } catch (Exception e) {
            Toast.makeText(this, "无法恢复编辑草稿：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void migrateLegacy() throws Exception {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String uri = prefs.getString(SettingsFragment.HOSTS_URI, "");
        boolean network = prefs.getBoolean(SettingsFragment.IS_NET, false);
        if (!network && uri.isEmpty()) return;
        String content;
        try (InputStream in = network ? openFileInput(SettingsFragment.NET_HOST_FILE)
                : getContentResolver().openInputStream(Uri.parse(uri))) {
            content = HostsGroupStore.readText(in);
        }
        groups.add(new HostsGroups.Group(UUID.randomUUID().toString(), "原有 hosts", content, true));
        HostsGroups.Snapshot snapshot = store.save(groups);
        DnsChange.installHosts(snapshot);
    }

    private void render() {
        rows.removeAllViews();
        int enabled = 0;
        for (int i = 0; i < groups.size(); i++) {
            final int index = i;
            HostsGroups.Group group = groups.get(i);
            if (group.enabled) enabled++;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            Switch toggle = new Switch(this);
            toggle.setText(group.name);
            toggle.setTextSize(18);
            toggle.setMinHeight((int) (56 * getResources().getDisplayMetrics().density));
            toggle.setChecked(group.enabled);
            toggle.setEnabled(!importing);
            row.addView(toggle, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout contentPanel = new LinearLayout(this);
            contentPanel.setOrientation(LinearLayout.VERTICAL);
            EditText content = contentEditor();
            content.setContentDescription("hosts 内容 " + group.name);
            content.setText(drafts.containsKey(group.id) ? drafts.get(group.id) : group.content);
            content.setEnabled(!importing);
            content.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable value) { drafts.put(group.id, value.toString()); }
            });
            contentPanel.addView(content);
            LinearLayout actions = new LinearLayout(this);
            Button save = new Button(this);
            save.setText("保存内容");
            save.setContentDescription("保存内容 " + group.name);
            save.setEnabled(!importing);
            save.setOnClickListener(v -> {
                try {
                    HostsGroups.Group edited = group.withContent(content.getText().toString());
                    HostsGroups.requireValidContent(edited);
                    List<HostsGroups.Group> next = new ArrayList<>(groups);
                    next.set(index, edited);
                    if (apply(next)) {
                        drafts.remove(group.id);
                        Toast.makeText(this, "内容已保存", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) { showError(e); }
            });
            actions.addView(save, new LinearLayout.LayoutParams(0, -2, 1));
            Button cancel = new Button(this);
            cancel.setText("放弃修改");
            cancel.setContentDescription("放弃修改 " + group.name);
            cancel.setEnabled(!importing);
            cancel.setOnClickListener(v -> { drafts.remove(group.id); render(); });
            actions.addView(cancel, new LinearLayout.LayoutParams(0, -2, 1));
            contentPanel.addView(actions);
            contentPanel.setVisibility(expandedGroups.contains(group.id) ? View.VISIBLE : View.GONE);
            Button expand = new Button(this);
            expand.setText(expandedGroups.contains(group.id) ? "收起" : "展开");
            expand.setContentDescription((expandedGroups.contains(group.id) ? "收起分组 " : "展开分组 ") + group.name);
            expand.setOnClickListener(v -> {
                boolean open = !expandedGroups.contains(group.id);
                if (open) expandedGroups.add(group.id);
                else expandedGroups.remove(group.id);
                contentPanel.setVisibility(open ? View.VISIBLE : View.GONE);
                expand.setText(open ? "收起" : "展开");
                expand.setContentDescription((open ? "收起分组 " : "展开分组 ") + group.name);
            });
            row.addView(expand);
            Button edit = new Button(this);
            edit.setText("管理");
            edit.setContentDescription("管理分组 " + group.name);
            edit.setEnabled(!importing);
            row.addView(edit);
            toggle.setOnCheckedChangeListener((button, checked) -> {
                List<HostsGroups.Group> next = new ArrayList<>(groups);
                next.set(index, group.withEnabled(checked));
                apply(next);
            });
            edit.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle(group.name)
                    .setItems(new String[]{"重命名", "删除分组"}, (dialog, which) -> {
                        if (which == 0) rename(index);
                        else new AlertDialog.Builder(this).setTitle("删除 " + group.name + "？")
                                .setMessage("只删除应用内副本，不删除原 hosts 文件。")
                                .setNegativeButton("取消", null).setPositiveButton("删除", (d, w) -> {
                                    List<HostsGroups.Group> next = new ArrayList<>(groups);
                                    next.remove(index);
                                    apply(next);
                                }).show();
                    }).show());
            rows.addView(row);
            rows.addView(contentPanel);
        }
        status.setText("已启用 " + enabled + " / " + groups.size()
                + " 组。可组合开启；同域名 IP 冲突时拒绝更改。\n"
                + "有效映射变化时自动重启已开启的 VPN；关闭时只保存。内容修改后需点保存。"
                + (groups.isEmpty() ? "\n尚无分组，请导入 hosts 文件。" : ""));
    }

    private EditText contentEditor() {
        EditText editor = new EditText(this);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setTextSize(14);
        editor.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
        editor.setHintTextColor(ContextCompat.getColor(this, R.color.secondary_text));
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setMinLines(2);
        editor.setMaxLines(10);
        editor.setHint("192.0.2.33 member.example.com");
        return editor;
    }

    private void createGroup() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        fields.setPadding(padding, padding, padding, padding);
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("分组名称");
        name.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
        name.setHintTextColor(ContextCompat.getColor(this, R.color.secondary_text));
        name.setText(restoredName);
        fields.addView(name);
        EditText content = contentEditor();
        content.setText(restoredContent);
        creatingName = name;
        creatingContent = content;
        creating = true;
        content.setContentDescription("新分组 hosts 内容");
        fields.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("手动新建 hosts 分组")
                .setView(fields).setNegativeButton("取消", null).setPositiveButton("创建", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = name.getText().toString().trim();
            if (title.isEmpty() || title.length() > 80) { name.setError("请输入 1–80 字名称"); return; }
            try {
                HostsGroups.Group group = new HostsGroups.Group(UUID.randomUUID().toString(), title,
                        content.getText().toString(), false);
                HostsGroups.requireValidContent(group);
                List<HostsGroups.Group> next = new ArrayList<>(groups);
                next.add(group);
                if (apply(next)) dialog.dismiss();
            } catch (Exception e) { showError(e); }
        }));
        dialog.setOnDismissListener(d -> {
            creating = false;
            creatingName = null;
            creatingContent = null;
            restoredName = "";
            restoredContent = "";
        });
        dialog.show();
    }

    private void rename(int index) {
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setText(groups.get(index).name);
        name.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("重命名分组").setView(name)
                .setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty() || value.length() > 80) { name.setError("请输入 1–80 字名称"); return; }
            List<HostsGroups.Group> next = new ArrayList<>(groups);
            next.set(index, groups.get(index).withName(value));
            if (apply(next)) dialog.dismiss();
        }));
        dialog.show();
    }

    private boolean apply(List<HostsGroups.Group> next) {
        try {
            HostsGroups.Snapshot previous = HostsGroups.merge(groups);
            HostsGroups.Snapshot snapshot = store.save(next);
            groups = next;
            DnsChange.installHosts(snapshot);
            if (!previous.sameMappings(snapshot)) VhostsService.restartIfRunning(this);
            render();
            return true;
        } catch (Exception e) {
            render();
            showError(e);
            return false;
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != IMPORT || resultCode != RESULT_OK || data == null) return;
        List<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) uris.add(data.getData());
        importing = true;
        importButton.setEnabled(false);
        newButton.setEnabled(false);
        render();
        new Thread(() -> {
            try {
                List<HostsGroups.Group> next = new ArrayList<>(groups);
                long importedBytes = 0;
                for (Uri uri : uris) {
                    String name = "hosts";
                    try (Cursor cursor = getContentResolver().query(uri,
                            new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
                    }
                    if (name == null || name.trim().isEmpty()) name = "hosts";
                    String content;
                    try (InputStream in = getContentResolver().openInputStream(uri)) {
                        content = HostsGroupStore.readText(in);
                    }
                    importedBytes += content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                    if (importedBytes > 2 * 1024 * 1024)
                        throw new IllegalArgumentException("本次导入合计不能超过 2 MB");
                    HostsGroups.Group group = new HostsGroups.Group(UUID.randomUUID().toString(), name, content, true);
                    if (HostsGroups.merge(Collections.singletonList(group)).size() == 0)
                        throw new IllegalArgumentException(name + "：没有有效的 hosts 记录");
                    // New imports start off so mutually exclusive environments can be imported together.
                    next.add(group.withEnabled(false));
                }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    importing = false;
                    importButton.setEnabled(true);
                    newButton.setEnabled(true);
                    if (apply(next)) Toast.makeText(this, "已导入，分组默认关闭，请按需开启", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    importing = false;
                    importButton.setEnabled(true);
                    newButton.setEnabled(true);
                    render();
                    showError(e);
                });
            }
        }, "hosts-import").start();
    }

    private void showError(Exception e) {
        new AlertDialog.Builder(this).setTitle("未应用更改")
                .setMessage(e.getMessage() == null ? e.toString() : e.getMessage())
                .setPositiveButton("确定", null).show();
    }
}
