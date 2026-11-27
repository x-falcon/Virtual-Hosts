/*
 **Copyright (C) 2017  xfalcon
 **
 **This program is free software: you can redistribute it and/or modify
 **it under the terms of the GNU General Public License as published by
 **the Free Software Foundation, either version 3 of the License, or
 **(at your option) any later version.
 **
 **This program is distributed in the hope that it will be useful,
 **but WITHOUT ANY WARRANTY; without even the implied warranty of
 **MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **GNU General Public License for more details.
 **
 **You should have received a copy of the GNU General Public License
 **along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **
 */

package com.github.xfalcon.vhosts.vservice;

import com.github.xfalcon.vhosts.util.LogUtils;
import org.xbill.DNS.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import com.github.xfalcon.vhosts.groups.HostsGroups;
import com.github.xfalcon.vhosts.groups.HostsGroupStore;
import java.util.Collections;

public class DnsChange {

    static String TAG = DnsChange.class.getSimpleName();
    private static volatile HostsGroups.Snapshot hosts = HostsGroups.merge(Collections.emptyList());

    public static void installHosts(HostsGroups.Snapshot snapshot) {
        hosts = snapshot;
    }


    public static ByteBuffer handle_dns_packet(Packet packet) {
        HostsGroups.Snapshot snapshot = hosts;
        try {
            ByteBuffer packet_buffer = packet.backingBuffer;
            packet_buffer.mark();
            byte[] tmp_bytes = new byte[packet_buffer.remaining()];
            packet_buffer.get(tmp_bytes);
            packet_buffer.reset();
            Message message = new Message(tmp_bytes);
            org.xbill.DNS.Record question = message.getQuestion();
            int type = question.getType();
            if (type != Type.A && type != Type.AAAA) return null;
            Name query_domain = question.getName();
            String mapped = snapshot.lookup(query_domain.toString(), type == Type.AAAA);
            if (mapped == null) return null;
            if (!mapped.isEmpty()) {
                InetAddress address = Address.getByAddress(mapped);
                org.xbill.DNS.Record record = type == Type.A
                        ? new ARecord(query_domain, 1, 60, address)
                        : new AAAARecord(query_domain, 1, 60, address);
                message.addRecord(record, 1);
            }
            message.getHeader().setFlag(Flags.QR);
            packet_buffer.limit(packet_buffer.capacity());
            packet_buffer.put(message.toWire());
            packet_buffer.limit(packet_buffer.position());
            packet_buffer.reset();
            packet.swapSourceAndDestination();
            packet.updateUDPBuffer(packet_buffer, packet_buffer.remaining());
            packet_buffer.position(packet_buffer.limit());
            LogUtils.d(TAG, "hit: " + question.getType() + " :" + query_domain.toString() + " :" + mapped);
            return packet_buffer;
        } catch (Exception e) {
            LogUtils.d(TAG, "dns hook error", e);
            return null;
        }

    }

    public static HostsGroups.Snapshot parseHosts(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream) {
            String content = HostsGroupStore.readText(in);
            return HostsGroups.merge(Collections.singletonList(
                    new HostsGroups.Group("legacy", "原有 hosts", content, true)));
        }
    }

    public static int handle_hosts(InputStream inputStream) {
        try {
            HostsGroups.Snapshot parsed = parseHosts(inputStream);
            installHosts(parsed);
            return parsed.size();
        } catch (IOException | IllegalArgumentException e) {
            LogUtils.e(TAG, "读取 hosts 失败，保留原映射", e);
            return 0;
        }
    }
}
