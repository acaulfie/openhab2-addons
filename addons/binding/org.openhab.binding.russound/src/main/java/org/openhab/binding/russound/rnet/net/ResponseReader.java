package org.openhab.binding.russound.rnet.net;

import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public interface ResponseReader extends Runnable {

    void setQueueandChannel(BlockingQueue<Object> responses, AtomicReference<SocketChannel> socketChannel);

}
