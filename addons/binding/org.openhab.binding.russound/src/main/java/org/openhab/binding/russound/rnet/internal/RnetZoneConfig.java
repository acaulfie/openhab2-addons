package org.openhab.binding.russound.rnet.internal;

public class RnetZoneConfig {
    private int controller;
    private int zone;

    public void setController(int controller) {
        this.controller = controller;
    }

    public void setZone(int zone) {
        this.zone = zone;
    }

    public int getController() {
        return controller;
    }

    public int getZone() {
        return zone;
    }

}
