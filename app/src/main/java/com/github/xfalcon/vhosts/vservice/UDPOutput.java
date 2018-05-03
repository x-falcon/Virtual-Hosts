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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class UDPOutput implements Runnable
{
    private static final String TAG = UDPOutput.class.getSimpleName();

    private VhostsService vpnService;
    private ConcurrentLinkedQueue<Packet> inputQueue;
    private ConcurrentLinkedQueue<ByteBuffer> outputQueue;
    private Selector selector;
    private ReentrantLock udpSelectorLock;
    private StringBuilder stringBuild;


    private static final int MAX_CACHE_SIZE = 50;
    private LRUCache<String, DatagramChannel> channelCache =
            new LRUCache<>(MAX_CACHE_SIZE, new LRUCache.CleanupCallback<String, DatagramChannel>()
            {
                @Override
                public void cleanup(Map.Entry<String, DatagramChannel> eldest)
                {
                    closeChannel(eldest.getValue());
                }
            });

    public UDPOutput(ConcurrentLinkedQueue<Packet> inputQueue,ConcurrentLinkedQueue<ByteBuffer> outputQueue, Selector selector,ReentrantLock udpSelectorLock, VhostsService vpnService)
    {
        this.inputQueue = inputQueue;
        this.selector = selector;
        this.vpnService = vpnService;
        this.outputQueue=outputQueue;
        this.udpSelectorLock=udpSelectorLock;
        this.stringBuild=new StringBuilder(32);
    }

    @Override
    public void run() {
        LogUtils.i(TAG, "Started");
        try {

            while (!Thread.interrupted()) {

                Packet currentPacket = inputQueue.poll();
                if (currentPacket == null){
                    Thread.sleep(11);
                    continue;
                }
                // hook dns packet
                if(currentPacket.udpHeader.destinationPort==53){
                    ByteBuffer packet_buffer= DnsChange.handle_dns_packet(currentPacket);
                    if(packet_buffer!=null){
                        this.outputQueue.offer(packet_buffer);
                        continue;
                    }
                }
                InetAddress destinationAddress = currentPacket.ipHeader.destinationAddress;
                int destinationPort = currentPacket.udpHeader.destinationPort;
                int sourcePort = currentPacket.udpHeader.sourcePort;
                String ipAndPort=getStringBuild().append(destinationAddress.getHostAddress()).append(destinationPort).append(sourcePort).toString();
                DatagramChannel outputChannel = channelCache.get(ipAndPort);
                if (outputChannel == null) {
                    outputChannel = DatagramChannel.open();
                    vpnService.protect(outputChannel.socket());
                    try
                    {
                        outputChannel.connect(new InetSocketAddress(destinationAddress, destinationPort));
                    }
                    catch (IOException e)
                    {
                        LogUtils.e(TAG, "Connection error: " + ipAndPort, e);
                        closeChannel(outputChannel);
                        ByteBufferPool.release(currentPacket.backingBuffer);
                        continue;
                    }
                    outputChannel.configureBlocking(false);
                    currentPacket.swapSourceAndDestination();
                    udpSelectorLock.lock();
                    selector.wakeup();
                    outputChannel.register(selector, SelectionKey.OP_READ, currentPacket);
                    udpSelectorLock.unlock();
                    channelCache.put(ipAndPort, outputChannel);
                }

                try
                {
                    ByteBuffer payloadBuffer = currentPacket.backingBuffer;
                    while (payloadBuffer.hasRemaining())
                        outputChannel.write(payloadBuffer);
                }
                catch (IOException e)
                {
                    LogUtils.e(TAG, "Network write error: " + ipAndPort, e);
                    channelCache.remove(ipAndPort);
                    closeChannel(outputChannel);
                }
                ByteBufferPool.release(currentPacket.backingBuffer);
            }
        }
        catch (InterruptedException e)
        {
            LogUtils.i(TAG, "Stopping");
        }
        catch (IOException e)
        {
            LogUtils.i(TAG, e.toString(), e);
        }
        finally
        {
            closeAll();
        }
    }

    private void closeAll()
    {
        Iterator<Map.Entry<String, DatagramChannel>> it = channelCache.entrySet().iterator();
        while (it.hasNext())
        {
            closeChannel(it.next().getValue());
            it.remove();
        }
    }

    private void closeChannel(DatagramChannel channel)
    {
        try
        {
            channel.close();
        }
        catch (IOException e)
        {
            // Ignore
        }
    }

    private StringBuilder getStringBuild(){
        stringBuild.setLength(0);
        return stringBuild;
    }

}
