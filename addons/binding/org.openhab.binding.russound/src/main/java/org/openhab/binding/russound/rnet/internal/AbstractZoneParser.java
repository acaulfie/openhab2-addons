package org.openhab.binding.russound.rnet.internal;

public abstract class AbstractZoneParser implements BusParser {

    int controller;
    int zone;

    public AbstractZoneParser(int zone, int controller) {
        this.controller = controller;
        this.zone = zone;
    }
}
