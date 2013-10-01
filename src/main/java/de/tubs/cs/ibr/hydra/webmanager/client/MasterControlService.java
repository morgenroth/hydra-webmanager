package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

@RemoteServiceRelativePath("master")
public interface MasterControlService extends RemoteService {
    public Session getSession(Long id);
    public ArrayList<Session> getSessions();
    public ArrayList<Node> getNodes(String sessionKey);
    public ArrayList<Slave> getSlaves();
    public void triggerAction(Session s, Session.Action action);
}
