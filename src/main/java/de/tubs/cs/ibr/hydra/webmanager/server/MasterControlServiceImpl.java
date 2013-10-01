package de.tubs.cs.ibr.hydra.webmanager.server;

import java.util.ArrayList;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import de.tubs.cs.ibr.hydra.webmanager.client.MasterControlService;
import de.tubs.cs.ibr.hydra.webmanager.server.db.Database;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session.Action;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class MasterControlServiceImpl extends RemoteServiceServlet implements MasterControlService {

    /**
     * serial ID
     */
    private static final long serialVersionUID = 1516194997681942066L;

    @Override
    public void triggerAction(Session s, Action action) {
        // TODO: change state
        
        // TODO: send atmosphere broadcast
        
        // broadcast session change
        MasterServer.broadcast(new Event(EventType.SESSION_STATE_CHANGED));
    }

    @Override
    public Session getSession(Long id) {
        return Database.getInstance().getSession(id);
    }

    @Override
    public ArrayList<Session> getSessions() {
        return Database.getInstance().getSessions();
    }

    @Override
    public ArrayList<Node> getNodes(String sessionKey) {
        return Database.getInstance().getNodes(sessionKey);
    }

    @Override
    public ArrayList<Slave> getSlaves() {
        return MasterServer.getSlaves();
    }

}
