package org.openhab.binding.russound.rnet.internal;

public interface BusParser {

    public boolean matches(Byte[] bytes);

    public ZoneStateUpdate process(Byte[] bytes);

}
