package com.github.xfalcon.vhosts.groups;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Parses first, then publishes a complete immutable mapping to DNS readers. */
public final class HostsGroups {
    public static final class Group {
        public final String id, name, content;
        public final boolean enabled;
        public Group(String id, String name, String content, boolean enabled) {
            this.id = id;
            this.name = name;
            this.content = content;
            this.enabled = enabled;
        }
        public Group withEnabled(boolean value) { return new Group(id, name, content, value); }
        public Group withContent(String value) { return new Group(id, name, value, enabled); }
        public Group withName(String value) { return new Group(id, value, content, enabled); }
    }

    public static final class Snapshot {
        public final Map<String, String> ipv4, ipv6;
        private Snapshot(Map<String, String> ipv4, Map<String, String> ipv6) {
            this.ipv4 = Collections.unmodifiableMap(new LinkedHashMap<>(ipv4));
            this.ipv6 = Collections.unmodifiableMap(new LinkedHashMap<>(ipv6));
        }
        /** Null forwards upstream; empty suppresses the unmapped family for a mapped host. */
        public String lookup(String name, boolean v6) {
            Map<String, String> selected = v6 ? ipv6 : ipv4;
            Map<String, String> other = v6 ? ipv4 : ipv6;
            String key = name.toLowerCase(Locale.ROOT);
            if (!key.endsWith(".")) key += ".";
            if (selected.containsKey(key)) return selected.get(key);
            if (other.containsKey(key)) return "";
            key = "." + key;
            while (!key.equals(".")) {
                if (selected.containsKey(key)) return selected.get(key);
                if (other.containsKey(key)) return "";
                int next = key.indexOf('.', 1);
                if (next < 0) break;
                key = key.substring(next);
            }
            return null;
        }
        public boolean sameMappings(Snapshot other) {
            return ipv4.equals(other.ipv4) && ipv6.equals(other.ipv6);
        }
        public int size() { return ipv4.size() + ipv6.size(); }
    }

    public static Snapshot merge(List<Group> groups) {
        Map<String, String> v4 = new LinkedHashMap<>(), v6 = new LinkedHashMap<>();
        Map<String, String> owners = new LinkedHashMap<>();
        for (Group group : groups) {
            if (!group.enabled) continue;
            String[] lines = group.content.replace("\ufeff", "").split("\\r?\\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].split("#", 2)[0].trim();
                if (line.isEmpty()) continue;
                String[] fields = line.split("\\s+");
                String where = group.name + ":" + (i + 1);
                if (fields.length < 2) throw new IllegalArgumentException(where + "：缺少域名");
                String ip = numericAddress(fields[0], where);
                boolean isV6 = ip.contains(":");
                Map<String, String> target = isV6 ? v6 : v4;
                for (int j = 1; j < fields.length; j++) {
                    String domain = fields[j].toLowerCase(Locale.ROOT);
                    if (domain.endsWith(".")) domain = domain.substring(0, domain.length() - 1);
                    String check = domain.startsWith(".") ? domain.substring(1) : domain;
                    if (check.isEmpty() || check.length() > 253) throw new IllegalArgumentException(where + "：无效域名 " + domain);
                    for (String label : check.split("\\.", -1)) {
                        if (!label.matches("[a-z0-9_](?:[a-z0-9_-]{0,61}[a-z0-9_])?"))
                            throw new IllegalArgumentException(where + "：无效域名 " + domain);
                    }
                    String key = domain + ".";
                    String old = target.get(key);
                    String ownerKey = (isV6 ? "6:" : "4:") + key;
                    if (old != null && !old.equals(ip))
                        throw new IllegalArgumentException("域名冲突 " + domain + "：" + owners.get(ownerKey)
                                + " → " + old + "；" + group.name + " → " + ip);
                    target.put(key, ip);
                    owners.put(ownerKey, group.name);
                }
            }
        }
        return new Snapshot(v4, v6);
    }

    public static void requireValidContent(Group group) {
        if (merge(Collections.singletonList(group.withEnabled(true))).size() == 0)
            throw new IllegalArgumentException(group.name + "：请至少填写一条有效 hosts 记录");
    }

    private static String numericAddress(String value, String where) {
        try {
            if (value.contains(":")) {
                // Colon plus numeric-only characters prevents accidental hostname resolution.
                if (!value.matches("[0-9a-fA-F:.]+")) throw new IllegalArgumentException();
                return InetAddress.getByName(value).getHostAddress();
            }
            String[] parts = value.split("\\.", -1);
            if (parts.length != 4) throw new IllegalArgumentException();
            StringBuilder normalized = new StringBuilder();
            for (String part : parts) {
                if (!part.matches("[0-9]{1,3}")) throw new IllegalArgumentException();
                int n = Integer.parseInt(part);
                if (n > 255) throw new IllegalArgumentException();
                if (normalized.length() > 0) normalized.append('.');
                normalized.append(n);
            }
            return normalized.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(where + "：无效 IP " + value);
        }
    }
}
