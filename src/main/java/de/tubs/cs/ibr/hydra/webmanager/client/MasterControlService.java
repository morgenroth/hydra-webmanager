package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

@RemoteServiceRelativePath("master")
public interface MasterControlService extends RemoteService {
    /**
     * Create a new session
     * @return A session object with a unique key
     */
    public Session createSession();
    
    /**
     * Store writable values of the session in the database
     * @param s
     */
    public void applySession(Session s);
    
    /**
     * Get the session data of the given session id
     * @param id Session ID of the requested data
     * @return A session object
     */
    public Session getSession(Long id);
    
    /**
     * Get all sessions
     * @return
     */
    public ArrayList<Session> getSessions();
    
    /**
     * Get all nodes related to the given sessionKey
     * @param sessionKey
     * @return
     */
    public ArrayList<Node> getNodes(String sessionKey);
    
    /**
     * Get a list of all connected slaves
     * @return
     */
    public ArrayList<Slave> getSlaves();
    
    /**
     * Execute an action on a session
     * @param s
     * @param action
     */
    public void triggerAction(Session s, Session.Action action);
    
    /**
     * Get all available image files
     * @return
     */
    public ArrayList<String> getAvailableImages();
}
