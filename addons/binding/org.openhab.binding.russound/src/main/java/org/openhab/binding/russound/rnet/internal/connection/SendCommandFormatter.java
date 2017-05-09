package org.openhab.binding.russound.rnet.internal.connection;

public interface SendCommandFormatter {

    public byte[] processCommand(byte[] bytes);
}
