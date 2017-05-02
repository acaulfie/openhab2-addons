package org.openhab.binding.russound.rnet.net;

import java.io.IOException;

public interface SessionListener<T> {
    /**
     * Called when a command has completed with the response for the command
     *
     * @param response a non-null, possibly empty response
     * @throws InterruptedException if the response processing was interrupted
     */
    public void responseReceived(T response) throws InterruptedException;

    /**
     * Called when a command finished with an exception or a general exception occurred while reading
     *
     * @param e a non-null io exception
     * @throws InterruptedException if the exception processing was interrupted
     */
    public void responseException(IOException e) throws InterruptedException;

}
