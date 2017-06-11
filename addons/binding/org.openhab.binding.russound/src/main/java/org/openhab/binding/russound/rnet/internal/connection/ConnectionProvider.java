package org.openhab.binding.russound.rnet.internal.connection;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public interface ConnectionProvider {

    public ObjectOutputStream getOutputStream();

    public DataInputStream getInputStream();

    public boolean isConnected();

    public boolean connect() throws NoConnectionException;

    public void disconnect() throws IOException;
}
