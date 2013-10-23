package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import de.tubs.cs.ibr.hydra.webmanager.client.MasterControlService;
import de.tubs.cs.ibr.hydra.webmanager.server.data.Configuration;
import de.tubs.cs.ibr.hydra.webmanager.server.data.Database;
import de.tubs.cs.ibr.hydra.webmanager.server.data.SessionContainer;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.Link;
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
                        if (!Session.State.ABORTED.equals(session.state)
                                && !Session.State.FINISHED.equals(session.state)
                                && !Session.State.ERROR.equals(session.state))
                            break;
                        
                        // set new state in database
                        Database.getInstance().setState(session, Session.State.DRAFT);

                        break;
                        
                    case REMOVE:
                        // check if transition is allowed
                        if (!Session.State.DRAFT.equals(session.state)
                                && !Session.State.INITIAL.equals(session.state))
                            break;
                        
                        // set new state in database
                        Database.getInstance().removeSession(session);
                        
                        break;
                        
                    case CANCEL:
                        // check if transition is allowed
                        if (!Session.State.PENDING.equals(session.state))
                            break;
                        
                        // set new state in database
                        Database.getInstance().setState(session, Session.State.CANCELLED);
                        
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
        return MasterServer.getSession(id);
    }

    @Override
    public ArrayList<Session> getSessions() {
        return Database.getInstance().getSessions();
    }

    @Override
    public ArrayList<Node> getNodes(Long sessionId) {
        Session s = null;
        if (sessionId != null) {
            s = new Session(sessionId);
        }
        
        return MasterServer.getNodes(s);
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

    @Override
    public Session createSession() {
        // create a new session in the database
        final Session s = Database.getInstance().createSession();
        
        final SessionContainer sc = SessionContainer.getContainer(s);
        
        MasterServer.invoke(new Task() {
            @Override
            public void run() {
                try {
                    // trigger initialization of the session
                    sc.initialize(SessionContainer.getDefault());
                    
                    // set session state to DRAFT
                    Database.getInstance().setState(s, Session.State.DRAFT);
                } catch (IOException e) {
                    // set session state to ERROR
                    Database.getInstance().setState(s, Session.State.ERROR);
                }
            }
        });
        
        return s;
    }

    @Override
    public void applySession(Session s) {
        // get session container
        SessionContainer sc = SessionContainer.getContainer(s);
        
        try {
            // try to initialize the container
            sc.initialize(null);
            
            // store configuration
            sc.apply(s);
        } catch (IOException e) {
            // can not initialize the container
        }
        
        // update database
        Database.getInstance().updateSession(s);
        
        // prepare session change extras
        MasterServer.fireSessionDataUpdated(s);
    }

    @Override
    public void applyNodes(ArrayList<Node> nodes) {
        HashSet<Long> sessions = new HashSet<Long>();
        
        Database d = Database.getInstance();
        for (Node n : nodes) {
            d.updateNode(n);
            sessions.add(n.sessionId);
        }
        
        for (Long session_id : sessions) {
            MasterServer.fireNodeStateChanged(session_id, null);
        }
    }

    @Override
    public void removeNodes(ArrayList<Node> nodes) {
        HashSet<Long> sessions = new HashSet<Long>();
        
        Database d = Database.getInstance();
        for (Node n : nodes) {
            d.removeNode(n);
            sessions.add(n.sessionId);
        }
        
        for (Long session_id : sessions) {
            MasterServer.fireNodeStateChanged(session_id, null);
        }
    }

    @Override
    public void createNodes(Long amount, Long sessionId, Long slaveId) {
        Database db = Database.getInstance();
        
        for (int i = 0; i < amount; i++) {
            db.createNode(sessionId, slaveId);
        }
        
        // prepare session change extras
        MasterServer.fireNodeStateChanged(sessionId, null);
    }

    @Override
    public ArrayList<DataPoint> getStatsData(Session s, Node n, Date begin, Date end) {
        Database db = Database.getInstance();
        return db.getStats(s, n, begin, end);
    }

    @Override
    public HashMap<Long, DataPoint> getStatsLatest(Session s) {
        Database db = Database.getInstance();
        return db.getStatsLatest(s);
    }

    @Override
    public ArrayList<Link> getLinks(Long sessionId) {
        return MasterServer.getLinks(sessionId);
    }
}
