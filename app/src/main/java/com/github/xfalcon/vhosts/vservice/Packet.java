/*
** Copyright 2015, Mohamed Naufal
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.github.xfalcon.vhosts.vservice;


import com.github.xfalcon.vhosts.util.LogUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Representation of an IP Packet
 */
// TODO: Reduce public mutability
public class Packet {
    private static final int IP4_HEADER_SIZE = 20;
    private static final int IP6_HEADER_SIZE = 40;
    private static final int TCP_HEADER_SIZE = 20;
    private static final int UDP_HEADER_SIZE = 8;
    private static final int TCP = 6;
    private static final int UDP = 17;
    private int IP_HEADER_SIZE;
    public int IP_TRAN_SIZE;
    public IPHeader ipHeader;
    public TCPHeader tcpHeader;
    public UDPHeader udpHeader;
    public ByteBuffer backingBuffer;

    private boolean isTCP;
    private boolean isUDP;

    public Packet(ByteBuffer buffer) throws UnknownHostException {
        byte versionAndIHL = buffer.get();
        byte version = (byte) (versionAndIHL >> 4);
        if (version == 4) {
            IP_HEADER_SIZE = IP4_HEADER_SIZE;
            byte IHL = (byte) (versionAndIHL & 0x0F);
            int headerLength = IHL << 2;
            this.ipHeader = new IP4Header(buffer, version, IHL, headerLength);
        } else if (version == 6) {
            IP_HEADER_SIZE = IP6_HEADER_SIZE;
            this.ipHeader = new IP6Header(buffer, version);
        } else {
            LogUtils.d("Un Know Packet", version + "");
            this.isTCP = false;
            this.isUDP = false;
            return;
        }
        if (this.ipHeader.protocol == TCP) {
            this.tcpHeader = new TCPHeader(buffer);
            this.isTCP = true;
            this.IP_TRAN_SIZE = IP_HEADER_SIZE + TCP_HEADER_SIZE;
        } else if (ipHeader.protocol == UDP) {
            this.udpHeader = new UDPHeader(buffer);
            this.isUDP = true;
            this.IP_TRAN_SIZE = IP_HEADER_SIZE + UDP_HEADER_SIZE;
        }
        this.backingBuffer = buffer;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Packet{");
        sb.append("IpHeader=").append(ipHeader);
        if (isTCP) sb.append(", tcpHeader=").append(tcpHeader);
        else if (isUDP) sb.append(", udpHeader=").append(udpHeader);
        sb.append(", payloadSize=").append(backingBuffer.limit() - backingBuffer.position());
        sb.append('}');
        return sb.toString();
    }

    public boolean isTCP() {
        return isTCP;
    }

    public boolean isUDP() {
        return isUDP;
    }

    public void swapSourceAndDestination() {
        InetAddress newSourceAddress = ipHeader.destinationAddress;
        ipHeader.destinationAddress = ipHeader.sourceAddress;
        ipHeader.sourceAddress = newSourceAddress;

        if (isUDP) {
            int newSourcePort = udpHeader.destinationPort;
            udpHeader.destinationPort = udpHeader.sourcePort;
            udpHeader.sourcePort = newSourcePort;
        } else if (isTCP) {
            int newSourcePort = tcpHeader.destinationPort;
            tcpHeader.destinationPort = tcpHeader.sourcePort;
            tcpHeader.sourcePort = newSourcePort;
        }
    }

    public void updateTCPBuffer(ByteBuffer buffer, byte flags, long sequenceNum, long ackNum, int payloadSize) {
        buffer.position(0);
        fillHeader(buffer);
        backingBuffer = buffer;

        tcpHeader.flags = flags;
        backingBuffer.put(IP_HEADER_SIZE + 13, flags);

        tcpHeader.sequenceNumber = sequenceNum;
        backingBuffer.putInt(IP_HEADER_SIZE + 4, (int) sequenceNum);

        tcpHeader.acknowledgementNumber = ackNum;
        backingBuffer.putInt(IP_HEADER_SIZE + 8, (int) ackNum);

        // Reset header size, since we don't need options
        byte dataOffset = (byte) (TCP_HEADER_SIZE << 2);
        tcpHeader.dataOffsetAndReserved = dataOffset;
        backingBuffer.put(IP_HEADER_SIZE + 12, dataOffset);
        checksum(payloadSize);
        int totalLength = TCP_HEADER_SIZE + payloadSize;
        ipHeader.updateIpHeader(this, totalLength);
    }

    public void updateUDPBuffer(ByteBuffer buffer, int payloadSize) {
        buffer.position(0);
        fillHeader(buffer);
        backingBuffer = buffer;

        int udpTotalLength = UDP_HEADER_SIZE + payloadSize;
        backingBuffer.putShort(IP_HEADER_SIZE + 4, (short) udpTotalLength);
        udpHeader.length = udpTotalLength;
        checksum(payloadSize);
        ipHeader.updateIpHeader(this, udpTotalLength);
    }

    private void fillHeader(ByteBuffer buffer) {
        ipHeader.fillHeader(buffer);
        if (isUDP)
            udpHeader.fillHeader(buffer);
        else if (isTCP)
            tcpHeader.fillHeader(buffer);
    }

    private void checksum(int payloadSize) {
        int sum = 0;
        int length;
        int pos;
        if (isTCP()) {
            length = TCP_HEADER_SIZE + payloadSize;
            pos = 16;
        } else {
            length = UDP_HEADER_SIZE + payloadSize;
            pos = 6;
        }
        ByteBuffer buffer;
        // Calculate pseudo-header checksum
        if (this.ipHeader.version == 4) {
            if (isUDP()) {
                backingBuffer.putShort(IP_HEADER_SIZE + 6, (short) 0);
                udpHeader.checksum = 0;
                return;
            }
            buffer = ByteBuffer.wrap(ipHeader.sourceAddress.getAddress());
            sum = BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());
            buffer = ByteBuffer.wrap(ipHeader.destinationAddress.getAddress());
            sum += BitUtils.getUnsignedShort(buffer.getShort()) + BitUtils.getUnsignedShort(buffer.getShort());
            sum += ipHeader.protocol + length;
        } else if (this.ipHeader.version == 6) {
            final int bbLength = 38; // IPv6 src + dst + nextHeader (with padding) + length 16+16+2+4
            buffer = ByteBufferPool.acquire();
            buffer.put(ipHeader.sourceAddress.getAddress());
            buffer.put(ipHeader.destinationAddress.getAddress());
            buffer.put((byte) 0); // padding
            buffer.put(ipHeader.protocol);
            buffer.putInt(length);
            buffer.rewind();
            for (int i = 0; i < bbLength / 2; ++i) {
                sum += 0xffff & buffer.getShort();
            }
            ByteBufferPool.release(buffer);
        }
        buffer = backingBuffer.duplicate();
        // Clear previous checksum
        buffer.putShort(IP_HEADER_SIZE + pos, (short) 0);

        // Calculate TCP segment checksum
        buffer.position(IP_HEADER_SIZE);
        while (length > 1) {
            sum += BitUtils.getUnsignedShort(buffer.getShort());
            length -= 2;
        }
        if (length > 0)
            sum += BitUtils.getUnsignedByte(buffer.get()) << 8;

        while (sum >> 16 > 0)
            sum = (sum & 0xFFFF) + (sum >> 16);

        sum = ~sum;
        if (isUDP()) udpHeader.checksum = sum;
        else tcpHeader.checksum = sum;
        backingBuffer.putShort(IP_HEADER_SIZE + pos, (short) sum);
    }

    public static class IPHeader {
        public byte version;
        public byte protocol;
        public InetAddress sourceAddress;
        public InetAddress destinationAddress;
        public int totalLength;

        public void fillHeader(ByteBuffer buffer) {
        }

        ;

        public void updateIpHeader(Packet packet, int totalLength) {
        }

        ;
    }

    public static class IP4Header extends IPHeader {
        private byte IHL;
        private int headerLength;
        private short typeOfService;

        private int identificationAndFlagsAndFragmentOffset;

        private short TTL;
        private int headerChecksum;
        private static byte[] addressBytes = new byte[4];

        public int optionsAndPadding;


        private IP4Header(ByteBuffer buffer, byte version, byte IHL, int headerLength) throws UnknownHostException {
            this.version = version;
            this.IHL = IHL;
            this.headerLength = headerLength;

            this.typeOfService = BitUtils.getUnsignedByte(buffer.get());
            this.totalLength = BitUtils.getUnsignedShort(buffer.getShort());

            this.identificationAndFlagsAndFragmentOffset = buffer.getInt();

            this.TTL = BitUtils.getUnsignedByte(buffer.get());
            this.protocol = buffer.get();
            this.headerChecksum = BitUtils.getUnsignedShort(buffer.getShort());
            buffer.get(addressBytes);
            this.sourceAddress = InetAddress.getByAddress(addressBytes);
            buffer.get(addressBytes);
            this.destinationAddress = InetAddress.getByAddress(addressBytes);

            //this.optionsAndPadding = buffer.getInt();
        }

        public void fillHeader(ByteBuffer buffer) {
            buffer.put((byte) (this.version << 4 | this.IHL));
            buffer.put((byte) this.typeOfService);
            buffer.putShort((short) this.totalLength);

            buffer.putInt(this.identificationAndFlagsAndFragmentOffset);

            buffer.put((byte) this.TTL);
            buffer.put(this.protocol);
            buffer.putShort((short) this.headerChecksum);

            buffer.put(this.sourceAddress.getAddress());
            buffer.put(this.destinationAddress.getAddress());
        }

        @Override
        public void updateIpHeader(Packet packet, int tcpPayLength) {
            this.totalLength = packet.IP_HEADER_SIZE + tcpPayLength;
            packet.backingBuffer.putShort(2, (short) this.totalLength);
            ByteBuffer buffer = packet.backingBuffer.duplicate();
            buffer.position(0);

            // Clear previous checksum
            buffer.putShort(10, (short) 0);

            int ipLength = headerLength;
            int sum = 0;
            while (ipLength > 0) {
                sum += BitUtils.getUnsignedShort(buffer.getShort());
                ipLength -= 2;
            }
            while (sum >> 16 > 0)
                sum = (sum & 0xFFFF) + (sum >> 16);

            sum = ~sum;
            headerChecksum = sum;
            packet.backingBuffer.putShort(10, (short) sum);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("IP4Header{");
            sb.append("version=").append(version);
            sb.append(", IHL=").append(IHL);
            sb.append(", typeOfService=").append(typeOfService);
            sb.append(", totalLength=").append(totalLength);
            sb.append(", identificationAndFlagsAndFragmentOffset=").append(identificationAndFlagsAndFragmentOffset);
            sb.append(", TTL=").append(TTL);
            sb.append(", protocol=").append(protocol);
            sb.append(", headerChecksum=").append(headerChecksum);
            sb.append(", sourceAddress=").append(sourceAddress.getHostAddress());
            sb.append(", destinationAddress=").append(destinationAddress.getHostAddress());
            sb.append('}');
            return sb.toString();
        }
    }

    public static class IP6Header extends IPHeader {
        private long versionTrafficFlowLabel;
        private byte hotLimit;
        private static byte[] addressBytes = new byte[16];

        private IP6Header(ByteBuffer buffer, byte version) throws UnknownHostException {
            this.version = version;
            buffer.position(0);
            this.versionTrafficFlowLabel = BitUtils.getUnsignedInt(buffer.getInt());
            //ipv6 payload == totalLength
            this.totalLength = BitUtils.getUnsignedShort(buffer.getShort());
            this.protocol = buffer.get();
            this.hotLimit = buffer.get();
            buffer.get(addressBytes);
            this.sourceAddress = InetAddress.getByAddress(addressBytes);
            buffer.get(addressBytes);
            this.destinationAddress = InetAddress.getByAddress(addressBytes);
            //this.optionsAndPadding = buffer.getInt();
        }

        @Override
        public void updateIpHeader(Packet packet, int totalLength) {
            packet.backingBuffer.putShort(4, (short) totalLength);
            this.totalLength = totalLength;
        }

        public void fillHeader(ByteBuffer buffer) {
            buffer.putInt((int) this.versionTrafficFlowLabel);
            buffer.putShort((short) this.totalLength);
            buffer.put(this.protocol);
            buffer.put(this.hotLimit);
            buffer.put((this.sourceAddress.getAddress()));
            buffer.put(this.destinationAddress.getAddress());
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("IP6Header{");
            sb.append("version=").append(version);
            sb.append(", trafficClassFlowLable=").append(versionTrafficFlowLabel);
            sb.append(", payload=").append(totalLength);
            sb.append(", protocol=").append(protocol);
            sb.append(", hotLimit=").append(hotLimit);
            sb.append(", sourceAddress=").append(sourceAddress.getHostAddress());
            sb.append(", destinationAddress=").append(destinationAddress.getHostAddress());
            sb.append('}');
            return sb.toString();
        }

    }

    public static class TCPHeader {
        public static final int FIN = 0x01;
        public static final int SYN = 0x02;
        public static final int RST = 0x04;
        public static final int PSH = 0x08;
        public static final int ACK = 0x10;
        public static final int URG = 0x20;

        public int sourcePort;
        public int destinationPort;

        public long sequenceNumber;
        public long acknowledgementNumber;

        private byte dataOffsetAndReserved;
        private int headerLength;
        private byte flags;
        private int window;

        private int checksum;
        private int urgentPointer;

        private byte[] optionsAndPadding;

        private TCPHeader(ByteBuffer buffer) {
            this.sourcePort = BitUtils.getUnsignedShort(buffer.getShort());
            this.destinationPort = BitUtils.getUnsignedShort(buffer.getShort());

            this.sequenceNumber = BitUtils.getUnsignedInt(buffer.getInt());
            this.acknowledgementNumber = BitUtils.getUnsignedInt(buffer.getInt());

            this.dataOffsetAndReserved = buffer.get();
            this.headerLength = (this.dataOffsetAndReserved & 0xF0) >> 2;
            this.flags = buffer.get();
            this.window = BitUtils.getUnsignedShort(buffer.getShort());

            this.checksum = BitUtils.getUnsignedShort(buffer.getShort());
            this.urgentPointer = BitUtils.getUnsignedShort(buffer.getShort());

            int optionsLength = this.headerLength - TCP_HEADER_SIZE;
            if (optionsLength > 0) {
                optionsAndPadding = new byte[optionsLength];
                buffer.get(optionsAndPadding, 0, optionsLength);
            }
        }

        public boolean isFIN() {
            return (flags & FIN) == FIN;
        }

        public boolean isSYN() {
            return (flags & SYN) == SYN;
        }

        public boolean isRST() {
            return (flags & RST) == RST;
        }

        public boolean isPSH() {
            return (flags & PSH) == PSH;
        }

        public boolean isACK() {
            return (flags & ACK) == ACK;
        }

        public boolean isURG() {
            return (flags & URG) == URG;
        }

        private void fillHeader(ByteBuffer buffer) {
            buffer.putShort((short) sourcePort);
            buffer.putShort((short) destinationPort);

            buffer.putInt((int) sequenceNumber);
            buffer.putInt((int) acknowledgementNumber);

            buffer.put(dataOffsetAndReserved);
            buffer.put(flags);
            buffer.putShort((short) window);

            buffer.putShort((short) checksum);
            buffer.putShort((short) urgentPointer);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("TCPHeader{");
            sb.append("sourcePort=").append(sourcePort);
            sb.append(", destinationPort=").append(destinationPort);
            sb.append(", sequenceNumber=").append(sequenceNumber);
            sb.append(", acknowledgementNumber=").append(acknowledgementNumber);
            sb.append(", headerLength=").append(headerLength);
            sb.append(", window=").append(window);
            sb.append(", checksum=").append(checksum);
            sb.append(", flags=");
            if (isFIN()) sb.append(" FIN");
            if (isSYN()) sb.append(" SYN");
            if (isRST()) sb.append(" RST");
            if (isPSH()) sb.append(" PSH");
            if (isACK()) sb.append(" ACK");
            if (isURG()) sb.append(" URG");
            sb.append('}');
            return sb.toString();
        }
    }

    public static class UDPHeader {
        public int sourcePort;
        public int destinationPort;

        private int length;
        private int checksum;

        private UDPHeader(ByteBuffer buffer) {
            this.sourcePort = BitUtils.getUnsignedShort(buffer.getShort());
            this.destinationPort = BitUtils.getUnsignedShort(buffer.getShort());

            this.length = BitUtils.getUnsignedShort(buffer.getShort());
            this.checksum = BitUtils.getUnsignedShort(buffer.getShort());
        }

        private void fillHeader(ByteBuffer buffer) {
            buffer.putShort((short) this.sourcePort);
            buffer.putShort((short) this.destinationPort);

            buffer.putShort((short) this.length);
            buffer.putShort((short) this.checksum);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("UDPHeader{");
            sb.append("sourcePort=").append(sourcePort);
            sb.append(", destinationPort=").append(destinationPort);
            sb.append(", length=").append(length);
            sb.append(", checksum=").append(checksum);
            sb.append('}');
            return sb.toString();
        }
    }

    private static class BitUtils {
        private static short getUnsignedByte(byte value) {
            return (short) (value & 0xFF);
        }

        private static int getUnsignedShort(short value) {
            return value & 0xFFFF;
        }

        private static long getUnsignedInt(int value) {
            return value & 0xFFFFFFFFL;
        }
    }
}
