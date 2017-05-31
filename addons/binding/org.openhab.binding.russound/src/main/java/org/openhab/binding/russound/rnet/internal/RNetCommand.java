package org.openhab.binding.russound.rnet.internal;

public interface RNetCommand {
    public Byte[] getCommand(ZoneId zoneId, byte value);
}
