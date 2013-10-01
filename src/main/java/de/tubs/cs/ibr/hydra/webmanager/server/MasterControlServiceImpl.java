package de.tubs.cs.ibr.hydra.webmanager.server;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcasterFactory;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import de.tubs.cs.ibr.hydra.webmanager.client.MasterControlService;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session.Action;

public class MasterControlServiceImpl extends RemoteServiceServlet implements MasterControlService {

    /**
     * serial ID
     */
    private static final long serialVersionUID = 1516194997681942066L;

    @Override
    public void triggerAction(Session s, Action action) {
        // TODO: change state
        
        // TODO: send atmosphere broadcast
        
        // get/create atmosphere broadcast channel
        BroadcasterFactory bf = DefaultBroadcasterFactory.getDefault();
        Broadcaster channel = bf.lookup("events", true);
        channel.broadcast(new Event(EventType.SESSION_STATE_CHANGED));
    }

}
