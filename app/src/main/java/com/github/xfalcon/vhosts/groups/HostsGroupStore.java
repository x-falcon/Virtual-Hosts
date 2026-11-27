package com.github.xfalcon.vhosts.groups;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class HostsGroupStore {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final AtomicFile file;
    public HostsGroupStore(Context context) {
        file = new AtomicFile(new File(context.getFilesDir(), "hosts-groups.json"));
    }

    /** Null means legacy single-file mode; an empty list means explicitly no groups. */
    public List<HostsGroups.Group> load() throws Exception {
        if (!file.getBaseFile().exists() && !new File(file.getBaseFile() + ".bak").exists()) return null;
        JSONObject root;
        try (InputStream in = file.openRead()) { root = new JSONObject(readText(in)); }
        if (root.getInt("version") != 1) throw new IOException("不支持的分组文件版本");
        JSONArray array = root.getJSONArray("groups");
        List<HostsGroups.Group> groups = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            groups.add(new HostsGroups.Group(item.getString("id"), item.getString("name"),
                    item.getString("content"), item.getBoolean("enabled")));
        }
        return groups;
    }

    public HostsGroups.Snapshot save(List<HostsGroups.Group> groups) throws Exception {
        HostsGroups.Snapshot snapshot = HostsGroups.merge(groups);
        JSONArray array = new JSONArray();
        for (HostsGroups.Group group : groups) {
            array.put(new JSONObject().put("id", group.id).put("name", group.name)
                    .put("content", group.content).put("enabled", group.enabled));
        }
        byte[] bytes = new JSONObject().put("version", 1).put("groups", array).toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("分组配置合计不能超过 2 MB");
        FileOutputStream out = null;
        try {
            out = file.startWrite();
            out.write(bytes);
            file.finishWrite(out);
        } catch (Exception e) {
            if (out != null) file.failWrite(out);
            throw e;
        }
        return snapshot;
    }

    public static String readText(InputStream in) throws IOException {
        if (in == null) throw new IOException("无法读取文件");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = in.read(buffer)) != -1) {
            if (out.size() + length > MAX_BYTES) throw new IOException("文件不能超过 2 MB");
            out.write(buffer, 0, length);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
