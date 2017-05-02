package org.openhab.binding.russound.rnet.internal;

public class BassChangeParser extends AbstractZoneParser {

    public BassChangeParser(int zone, int controller) {
        super(zone, controller);
    }

    @Override
    public boolean matches(Byte[] bytes) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ZoneStateUpdate process(Byte[] bytes) {
        return null;
    }

}
