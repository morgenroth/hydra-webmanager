package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.IOException;
import java.util.ArrayList;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import de.tubs.cs.ibr.hydra.webmanager.client.MasterControlService;
import de.tubs.cs.ibr.hydra.webmanager.server.data.Configuration;
import de.tubs.cs.ibr.hydra.webmanager.server.data.Database;
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
    public void triggerAction(final Session s, final Action action) {
        MasterServer.invoke(new Task() {

            @Override
            public void run() {
                // refresh session object
                Session session = Database.getInstance().getSession(s.id);
                
                switch (action) {
                    case ABORT:
                        // check if transition is allowed
                        if (!Session.State.RUNNING.equals(session.state)) break;
                        
                        // set new state in database
                        Database.getInstance().setState(session, Session.State.ABORTED);
                        
                        break;
                        
                    case QUEUE:
                        // check if transition is allowed
                        if (!Session.State.DRAFT.equals(session.state)) break;
                        
                        // set new state in database
                        Database.getInstance().setState(session, Session.State.PENDING);

                        break;
                        
                    case RESET:
                        // check if transition is allowed
                        if (Session.State.RUNNING.equals(session.state) || Session.State.DRAFT.equals(session.state)) break;
                        
                        // set new state in database
                        Database.getInstance().setState(session, Session.State.DRAFT);

                        break;
                        
                    default:
                        // unknown action - do nothing
                        break;
                    
                }
            }
            
        });
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

    @Override
    public ArrayList<String> getAvailableImages() {
        try {
            return Configuration.getAvailableImages();
        } catch (IOException e) {
            // error - can not query the list of images
            return new ArrayList<String>();
        }
    }

}
