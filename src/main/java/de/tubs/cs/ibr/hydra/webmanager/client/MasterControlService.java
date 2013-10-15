package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

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
     * Add a number of nodes
     * @param amount
     * @param slaveId
     */
    public void createNodes(Long amount, Long sessionId, Long slaveId);
    
    /**
     * Get all nodes related to the given sessionKey
     * @param sessionKey
     * @return
     */
    public ArrayList<Node> getNodes(Long sessionId);
    
    /**
     * Store writable values of the nodes
     * @param s
     */
    public void applyNodes(ArrayList<Node> nodes);
    
    /**
     * Remove a list of nodes
     */
    public void removeNodes(ArrayList<Node> nodes);
    
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
    
    /**
     * Get statistical data
     * @param s The session the data belong to.
     * @param n The node the data belong to. May be null.
     * @param begin The begin of the selection range. May be null.
     * @param end The end of the selection range. May be null.
     * @return An hash-map of the JSON encoded data indexed by the node-id and timestamp.
     */
    public HashMap<Long, HashMap<Long, String>> getStatsData(Session s, Node n, Date begin, Date end);
    
    /**
     * Get the latest data records
     * @param The session the data belong to.
     * @return An hash-map of the JSON encoded data indexed by the node-id.
     */
    public HashMap<Long, String> getStatsLatest(Session s);
}
