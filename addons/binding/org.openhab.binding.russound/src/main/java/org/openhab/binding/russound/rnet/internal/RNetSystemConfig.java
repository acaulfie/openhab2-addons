package org.openhab.binding.russound.rnet.internal;

public class RNetSystemConfig {

    private String ipAddress;
    private int port;
    private int retryPolling;

    private int numControllers;
    private int zonesPer;

    public int getNumControllers() {
        return numControllers;
    }

    public void setNumControllers(int numControllers) {
        this.numControllers = numControllers;
    }

    public int getZonesPer() {
        return zonesPer;
    }

    public void setZonesPer(int zonesPer) {
        this.zonesPer = zonesPer;
    }

    public int getRetryPolling() {
        return retryPolling;
    }

    public void setRetryPolling(int retryPolling) {
        this.retryPolling = retryPolling;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
