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

import android.util.Log;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Address;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DnsChange {

    static String TAG = DnsChange.class.getSimpleName();
    static ConcurrentHashMap<String, String> DOMAINS_IP_MAPS = null;


    public static ByteBuffer handle_dns_packet(Packet packet) {
        if (DOMAINS_IP_MAPS == null) {
            Log.d(TAG, "DOMAINS_IP_MAPS IS　NULL　HOST FILE ERROR");
            return null;
        }
        try {
            ByteBuffer packet_buffer=packet.backingBuffer;
            packet_buffer.mark();
            byte[] tmp_bytes = new byte[packet_buffer.remaining()];
            packet_buffer.get(tmp_bytes);
            packet_buffer.reset();
            Message message = new Message(tmp_bytes);
            Name query_domain = message.getQuestion().getName();
            String query_string = query_domain.toString();
            Log.d(TAG, "query: " + query_domain);
            if (!DOMAINS_IP_MAPS.containsKey(query_string)) {
                query_string="."+query_string;
                int j=0;
                while (true){
                    int i=query_string.indexOf(".",j);
                    if (i==-1){
                        return null;
                    }
                    String str=query_string.substring(i);
                    if("".equals(str)){
                        return null;
                    }
                    if(DOMAINS_IP_MAPS.containsKey(str)){
                        query_string=str;
                        break;
                    }
                    j=i+1;
                }
            }
            InetAddress address = Address.getByAddress(DOMAINS_IP_MAPS.get(query_string));
            ARecord a_record = new ARecord(query_domain, 1, 86400, address);
            message.addRecord(a_record, 1);
            message.getHeader().setFlag(Flags.QR);
            packet_buffer.limit(packet_buffer.capacity());
            packet_buffer.put(message.toWire());
            packet_buffer.limit(packet_buffer.position());
            packet_buffer.reset();
            packet.swapSourceAndDestination();
            packet.updateUDPBuffer(packet_buffer, packet_buffer.remaining());
            packet_buffer.position(packet_buffer.limit());
            Log.d(TAG, "hit: " + query_domain.toString() + " " + address.getHostName());
            return packet_buffer;
        } catch (Exception e) {
            Log.d(TAG, "dns hook error", e);
            return null;
        }

    }

    public static void handle_hosts(InputStream inputStream) {
        String STR_COMMENT = "#";
        String HOST_PATTERN_STR = "^\\s*(" + STR_COMMENT + "?)\\s*(\\S*)\\s*([^" + STR_COMMENT + "]*)" + STR_COMMENT + "?(.*)$";
        Pattern HOST_PATTERN = Pattern.compile(HOST_PATTERN_STR);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream));
            String line;
            DOMAINS_IP_MAPS = new ConcurrentHashMap<>();
            while (!Thread.interrupted() && (line = reader.readLine()) != null) {
                if(line.length()>1000)continue;
                Matcher matcher = HOST_PATTERN.matcher(line);
                if (matcher.find()) {
                    String ip = matcher.group(2);
                    try {
                        Address.getByAddress(ip);
                    } catch (Exception e) {
                        continue;
                    }
                    DOMAINS_IP_MAPS.put(matcher.group(3).trim() + ".", ip.trim());
                }
            }
            reader.close();
            inputStream.close();
            Log.d(TAG, DOMAINS_IP_MAPS.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
