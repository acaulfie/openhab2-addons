package org.openhab.binding.isy.internal;

import java.util.Collection;
import java.util.List;

import org.openhab.binding.isy.internal.protocol.Property;

public interface OHIsyClient {
    // public void connect();
    //
    // public void disconnect();

    public boolean changeNodeState(String command, String value, String address);

    public boolean changeVariableState(String type, String id, int value);

    public boolean changeSceneState(String address, int value);

    public boolean changeProgramState(String programId, String command);

    public List<Node> getNodes();

    public Collection<Program> getPrograms();

    public List<Variable> getVariables();

    public List<Scene> getScenes();

    public Property getNodeStatus(String node);

}
