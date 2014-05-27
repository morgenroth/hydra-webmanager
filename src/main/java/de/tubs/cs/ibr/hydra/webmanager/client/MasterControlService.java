package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import de.tubs.cs.ibr.hydra.webmanager.shared.Credentials;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataFile;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.MapDataSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

@RemoteServiceRelativePath("master")
public interface MasterControlService extends RemoteService {
    
    /**
     * authenticate user
     */
    public Boolean authenticate(String username, String password);
    
    /**
     * Get user credentials
     */
    public Credentials getCredentials();
    
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
     * Get data to paint all nodes with links
     * @param sessionId
     * @return
     */
    public MapDataSet getMapData(Long sessionId);
    
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
     * @param n The node the data belong to.
     * @param begin The begin of the selection range. May be null.
     * @param end The end of the selection range. May be null.
     * @return An array-list of the data-points
     */
    public ArrayList<DataPoint> getStatsData(Session s, Node n, Date begin, Date end);
    
    /**
     * Get the latest data records
     * @param The session the data belong to.
     * @return An hash-map of the data points indexed by the node-id.
     */
    public HashMap<Long, DataPoint> getStatsLatest(Session s);
    
    /**
     * Get a list of trace files owned by the session
     * @param s The session of the files to list
     * @return A list of trace files
     */
    public ArrayList<DataFile> getSessionFiles(Session s, String tag);
    
    /**
     * Remove a session file
     * @param s
     * @param tag
     * @param filename
     */
    public void removeSessionFile(Session s, String tag, String filename);
}
