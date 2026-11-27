package com.github.xfalcon.vhosts.groups;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class HostsGroupsTest {
    private HostsGroups.Group group(String name, String content, boolean enabled) {
        return new HostsGroups.Group(name, name, content, enabled);
    }

    @Test public void aliasesCommentsAndCaseAreNormalized() {
        HostsGroups.Snapshot s = HostsGroups.merge(Collections.singletonList(group("one",
                "\ufeff# header\n  # disabled\n192.0.2.33 MEMBER.example.com. alias.test # note\n", true)));
        assertEquals(2, s.ipv4.size());
        assertEquals("192.0.2.33", s.ipv4.get("member.example.com."));
        assertEquals("192.0.2.33", s.ipv4.get("alias.test."));
    }

    @Test public void disabledGroupsDoNotConflictAndAllOffClearsMapping() {
        HostsGroups.Group a = group("test1", "192.0.2.35 member.example.com", true);
        HostsGroups.Group b = group("test2", "192.0.2.33 member.example.com", false);
        assertEquals("192.0.2.35", HostsGroups.merge(Arrays.asList(a,b)).ipv4.get("member.example.com."));
        assertTrue(HostsGroups.merge(Collections.singletonList(b)).ipv4.isEmpty());
    }

    @Test public void conflictReportsBothGroupsAndDomain() {
        try {
            HostsGroups.merge(Arrays.asList(group("测试1", "192.0.2.35 member.example.com", true),
                    group("测试2", "192.0.2.33 MEMBER.example.com", true)));
            fail("must reject conflict");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("member.example.com"));
            assertTrue(e.getMessage().contains("测试1"));
            assertTrue(e.getMessage().contains("测试2"));
        }
    }

    @Test public void duplicateAddressesAndDualStackAreAllowed() {
        HostsGroups.Snapshot s = HostsGroups.merge(Arrays.asList(
                group("a", "127.0.0.1 localhost\n::1 localhost", true),
                group("b", "127.0.0.1 localhost\n0:0:0:0:0:0:0:1 localhost", true)));
        assertEquals(1, s.ipv4.size());
        assertEquals(1, s.ipv6.size());
    }

    @Test public void wildcardDoesNotReplaceExactEntry() {
        HostsGroups.Snapshot s = HostsGroups.merge(Collections.singletonList(group("a",
                "127.0.0.1 .example.com\n127.0.0.2 www.example.com", true)));
        assertEquals(2, s.ipv4.size());
        assertEquals("127.0.0.1", s.ipv4.get(".example.com."));
    }

    @Test public void rejectsInvalidRowsWithoutDnsLookups() {
        for (String text : Arrays.asList("example.com target.test", "999.1.1.1 target.test",
                "127.0.0.1", "127.0.0.1 https://example.com", "127.0.0.1 bad..host")) {
            try { HostsGroups.merge(Collections.singletonList(group("bad",text,true))); fail(text); }
            catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("bad")); }
        }
    }

    @Test public void snapshotsCannotBeMutated() {
        HostsGroups.Snapshot s = HostsGroups.merge(Collections.singletonList(group("a", "127.0.0.1 a.test", true)));
        try { s.ipv4.clear(); fail("immutable"); }
        catch (UnsupportedOperationException expected) { }
    }
    @Test public void mappedIpv4DoesNotLeakThroughUpstreamIpv6() {
        HostsGroups.Snapshot s = HostsGroups.merge(Collections.singletonList(group("a",
                "192.0.2.33 member.example.com\n127.0.0.1 .example.com\n127.0.0.2 www.example.com", true)));
        assertEquals("", s.lookup("member.example.com.", true));
        assertEquals("192.0.2.33", s.lookup("member.example.com.", false));
        assertEquals("127.0.0.2", s.lookup("www.example.com.", false));
        assertEquals("127.0.0.1", s.lookup("sub.example.com.", false));
        assertNull(s.lookup("normal.test.", true));
    }
    @Test public void editingKeepsIdentityAndEnabledStateAndValidatesDisabledContent() {
        HostsGroups.Group original = group("one", "192.0.2.35 member.example.com", false);
        HostsGroups.Group edited = original.withContent("192.0.2.33 member.example.com");
        assertEquals(original.id, edited.id);
        assertFalse(edited.enabled);
        HostsGroups.requireValidContent(edited);
        try { HostsGroups.requireValidContent(original.withContent("999.1.1.1 invalid.test")); fail(); }
        catch (IllegalArgumentException expected) { }
        try { HostsGroups.requireValidContent(original.withContent("# empty")); fail(); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void onlyEffectiveMappingChangesRequireVpnRestart() {
        HostsGroups.Group a = group("one", "192.0.2.35 member.example.com", true);
        HostsGroups.Snapshot before = HostsGroups.merge(Collections.singletonList(a));
        assertTrue(before.sameMappings(HostsGroups.merge(Collections.singletonList(a.withName("renamed")))));
        assertTrue(before.sameMappings(HostsGroups.merge(Collections.singletonList(a.withContent(
                "# comment\n192.0.2.35 member.example.com")))));
        assertFalse(before.sameMappings(HostsGroups.merge(Collections.singletonList(a.withEnabled(false)))));
        assertFalse(before.sameMappings(HostsGroups.merge(Collections.singletonList(a.withContent(
                "192.0.2.33 member.example.com")))));
    }
}
