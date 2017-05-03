package org.openhab.binding.russound.rnet.internal;

public class RNetSystemConfig {

    private String ipAddress;
    private int port;
    private int retryPolling;

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
