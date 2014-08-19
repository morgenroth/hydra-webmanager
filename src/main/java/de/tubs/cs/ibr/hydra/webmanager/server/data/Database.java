
package de.tubs.cs.ibr.hydra.webmanager.server.data;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import de.tubs.cs.ibr.hydra.webmanager.server.MasterServer;
import de.tubs.cs.ibr.hydra.webmanager.server.Task;
import de.tubs.cs.ibr.hydra.webmanager.shared.Credentials;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;
import de.tubs.cs.ibr.hydra.webmanager.shared.User;

public class Database {

    static final Logger logger = Logger.getLogger(Database.class.getSimpleName());
    
    private final static String NODES_FIELDS = "id, slave, session, name, state, address, assigned_slave";
    private final static String SLAVES_FIELDS = "id, name, address, state, owner, capacity";
    private final static String SESSIONS_FIELDS = "sessions.id, sessions.user, users.name, sessions.name, sessions.created, sessions.started, sessions.aborted, sessions.finished, sessions.state";
    private final static String USERS_FIELDS = "id, name";
    
    private final static String QUERY_NODES = "SELECT " + NODES_FIELDS + " FROM nodes ORDER BY id;";
    private final static String QUERY_NODES_SESSION = "SELECT " + NODES_FIELDS + " FROM nodes WHERE session = ? ORDER BY id;";
    private final static String QUERY_NODES_SESSION_SLAVE = "SELECT " + NODES_FIELDS + " FROM nodes WHERE session = ? AND slave = ? ORDER BY id;";
    
    private final static String QUERY_NODE = "SELECT " + NODES_FIELDS + " FROM nodes WHERE id = ?;";
    
    private final static String QUERY_SLAVES = "SELECT " + SLAVES_FIELDS + " FROM slaves WHERE owner IS NULL OR owner = ?;";
    private final static String QUERY_SLAVE = "SELECT " + SLAVES_FIELDS + " FROM slaves WHERE id = ? LIMIT 0,1;";
    private final static String QUERY_SLAVE_NAME_ADDRESS = "SELECT " + SLAVES_FIELDS + " FROM slaves WHERE name = ? AND address = ? LIMIT 0,1;";
    
    private final static String QUERY_SESSIONS = "SELECT " + SESSIONS_FIELDS + " FROM sessions LEFT JOIN users ON (users.id = sessions.user);";
    private final static String QUERY_SESSION = "SELECT " + SESSIONS_FIELDS + " FROM sessions LEFT JOIN users ON (users.id = sessions.user) WHERE sessions.id = ?;";
    
    private final static String QUERY_USER = "SELECT " + USERS_FIELDS + " FROM users WHERE id = ?;";
    private final static String QUERY_USER_BY_NAME = "SELECT " + USERS_FIELDS + " FROM users WHERE name = ?;";
    
    private final static String QUERY_ADDRESS_ALLOCS = "SELECT DISTINCT `address` FROM nodes WHERE `address` IS NOT NULL ORDER BY `address`;";
    private final static String QUERY_SLAVE_ALLOCS = "SELECT `slaves`.`id`, `slaves`.`capacity`, COUNT(`slaves`.`id`) as allocation, `nodes`.`assigned_slave` FROM `slaves` LEFT JOIN `nodes` ON (`slaves`.`id` = `nodes`.`assigned_slave`) WHERE `slaves`.`state` != 'disconnected' GROUP BY `slaves`.`id`;";
    
    private final static String QUERY_STATS = "SELECT `timestamp`, `node`, `data` FROM stats WHERE session = ? ORDER BY `timestamp`;";
    private final static String QUERY_STATS_BY_NODE = "SELECT `timestamp`, `data` FROM stats WHERE session = ? AND node = ? AND (`timestamp` > ?) AND (`timestamp` <= ?) ORDER BY timestamp DESC LIMIT 0,60;";
    private final static String QUERY_STATS_OF = "SELECT node,timestamp,data FROM stats WHERE timestamp = ? AND session = ? ORDER BY node;";
    private final static String QUERY_STATS_TIMESTAMPS = "SELECT DISTINCT timestamp FROM stats WHERE session = ? ORDER BY timestamp DESC;";
    

    private final static String QUERY_USERSESSION= "SELECT `username`, `sessionid`, `expires` FROM `usersessions` WHERE sessionid = ? AND  expires > NOW() LIMIT 1";

    private final static String UPDATE_NODE_ADDRESS = "UPDATE nodes SET `address` = ? WHERE id = ?;";
    private final static String UPDATE_NODE_STATE = "UPDATE nodes SET `state` = ? WHERE id = ?;";
    private final static String UPDATE_NODE = "UPDATE nodes SET `slave` = ?, `name` = ? WHERE id = ?;";
    private final static String UPDATE_NODE_ASSIGNMENT = "UPDATE nodes SET `assigned_slave` = ? WHERE id = ?;";
    private final static String UPDATE_NODE_NAME = "UPDATE nodes SET `name` = ? WHERE id = ?;";
    
    private final static String UPDATE_SLAVE_STATE = "UPDATE slaves SET `state` = ? WHERE id = ?;";
    private final static String UPDATE_SLAVE_OWNER_CAPACITY = "UPDATE slaves SET `owner` = ?, `capacity` = ? WHERE id = ?;";
    private final static String UPDATE_SLAVES_RESET = "UPDATE slaves SET `state` = 'disconnected';";
    
    private final static String UPDATE_SESSION_NAME = "UPDATE sessions SET `name` = ? WHERE id = ?;";
    private final static String UPDATE_SESSION_STATE_ABORTED = "UPDATE sessions SET `state` = ?, aborted = NOW() WHERE id = ?;";
    private final static String UPDATE_SESSION_STATE_RUNNING = "UPDATE sessions SET `state` = ?, started = NOW() WHERE id = ?;";
    private final static String UPDATE_SESSION_STATE_FINISHED = "UPDATE sessions SET `state` = ?, finished = NOW() WHERE id = ?;";
    private final static String UPDATE_SESSION_STATE_DRAFT = "UPDATE sessions SET `state` = ?, started = NULL, aborted = NULL, finished = NULL WHERE id = ?;";
    private final static String UPDATE_SESSION_STATE = "UPDATE sessions SET `state` = ? WHERE id = ?;";
    
    private final static String UPDATE_NODES_CLEAR = "UPDATE nodes SET `assigned_slave` = NULL, `address` = NULL, `state` = 'draft';";
    private final static String UPDATE_NODES_CLEAR_SESSION = "UPDATE nodes SET `assigned_slave` = NULL, `address` = NULL, `state` = 'draft' WHERE session = ?;";
    
    private final static String UPDATE_SESSIONS_RESET = "UPDATE sessions SET `state` = 'error' WHERE `state` = 'pending' OR `state` = 'running' OR `state` = 'cancelled' OR `state` = 'initial'";
    
    private final static String DELETE_NODE = "DELETE FROM nodes WHERE id = ?;";
    private final static String DELETE_SESSION_NODES = "DELETE FROM nodes WHERE session = ?;";
    private final static String DELETE_SESSION = "DELETE FROM sessions WHERE id = ?;";
    private final static String DELETE_USERSESSION = "DELETE FROM usersessions WHERE sessionid = ?;";
    
    private final static String INSERT_NODE = "INSERT INTO nodes (`session`, `slave`) VALUES (?, ?);";
    private final static String INSERT_SLAVE = "INSERT INTO slaves (`name`, `address`, `owner`, `capacity`) VALUES (?, ?, ?, ?);";
    private final static String INSERT_SESSION = "INSERT INTO sessions (`user`, `created`) VALUES (?, NOW());";
    private final static String INSERT_STATS_DATA = "INSERT INTO stats (`session`, `node`, `data`) VALUES (?, ?, ?);";
    
    private final static String INSERT_USERSESSION= "INSERT INTO usersessions (`username`, `sessionid`, `expires`) VALUES (?, ?, ?);";
    
    private final static String INSERT_USER= "INSERT INTO users (`name`) VALUES (?);";

    private final static String PURGE_USERSESSION = "DELETE FROM usersessions WHERE `expires` < NOW() - 1000 ;";
    

    private static Database __db__ = new Database();
    
    //private Connection mConn = null;
    private boolean mInitialized = false;
    
    private final BlockingQueue<Connection> mConnPool = new LinkedBlockingQueue<Connection>();
    private static final int CONNECTION_POOL_SIZE = 5;
    
    public static Database getInstance() {
        return __db__;
    }
    
    public synchronized void initializePool() {
        if (!mInitialized) {
            // create connection pool
            for (int i = 0; i < CONNECTION_POOL_SIZE; i++) {
                Connection conn = createConnection();
                if (conn != null) mConnPool.offer( conn );
            }
            
            mInitialized = true;
        }
    }
    
    public Connection getConnection() {
        initializePool();
        
        try {
            return mConnPool.take();
        } catch (InterruptedException e) {
            return null;
        }
    }
    
    public void releaseConnection(Connection conn) {
        if (conn != null) mConnPool.offer(conn);
    }

    public synchronized void close() {
        if (mInitialized) {
            for (int i = 0; i < CONNECTION_POOL_SIZE; i++) {
                try {
                    mConnPool.take().close();
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mInitialized = false;
        }
    }

    private Connection createConnection() {
        Connection ret = null;
        String driver = "com.mysql.jdbc.Driver";

        try {
            Properties p = Configuration.getProperties();
            String url = p.getProperty("db.url");
            String dbname = p.getProperty("db.name");
            String username = p.getProperty("db.username");
            String password = p.getProperty("db.password");
            
            // look for the mysql driver
            Class.forName(driver).newInstance();
            
            // open db connection
            ret = DriverManager.getConnection(url + dbname + "?autoReconnect=true", username, password);
        } catch (SQLException e) {
            ret = null;
            logger.severe("Mysql Connection Error: " + e.getMessage());
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    private void transform(ResultSet rs, Node n) throws SQLException {
        n.id = rs.getLong(1);
        
        n.slaveId = rs.getLong(2);
        if (rs.wasNull()) n.slaveId = null;
        
        n.sessionId = rs.getLong(3);
        if (rs.wasNull()) n.sessionId = null;
        
        n.name = rs.getString(4);
        if (rs.wasNull()) n.name = null;
        
        n.state = Node.State.fromString(rs.getString(5));
        
        n.address = rs.getString(6);
        if (rs.wasNull()) n.address = null;
        
        n.assignedSlaveId = rs.getLong(7);
        if (rs.wasNull()) n.assignedSlaveId = null;
    }
    
    private void transform(ResultSet rs, Slave s) throws SQLException {
        s.name = rs.getString(2);
        s.address = rs.getString(3);
        if (rs.wasNull()) s.address = null;
        
        s.state = Slave.State.fromString(rs.getString(4));
        
        s.owner = rs.getLong(5);
        if (rs.wasNull()) s.owner = null;
        
        s.capacity = rs.getLong(6);
        if (rs.wasNull()) s.capacity = null;
    }
    
    private void transform(ResultSet rs, Session s) throws SQLException {
        s.userid = rs.getLong(2);
        s.username = rs.getString(3);
        
        s.name = rs.getString(4);
        if (rs.wasNull()) s.name = null;
        
        s.created = rs.getTimestamp(5);
        
        s.started = rs.getTimestamp(6);
        if (rs.wasNull()) s.started = null;
        
        s.aborted = rs.getTimestamp(7);
        if (rs.wasNull()) s.aborted = null;
        
        s.finished = rs.getTimestamp(8);
        if (rs.wasNull()) s.finished = null;
        
        s.state = Session.State.fromString(rs.getString(9));
    }
    
    public ArrayList<Node> getNodes(Session session) {
        return getNodes(session, null);
    }
    
    public ArrayList<Node> getNodes(Session session, Slave slave) {
        ArrayList<Node> ret = new ArrayList<Node>();
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return ret;
        
        try {
            String query = null;
            
            // select the right query
            if (session == null) {
                query = QUERY_NODES;
            }
            else if (slave == null) {
                query = QUERY_NODES_SESSION;
            }
            else {
                query = QUERY_NODES_SESSION_SLAVE;
            }
            
            // create a statement
            try (PreparedStatement st = conn.prepareStatement(query)) {
                if (session != null) {
                    st.setLong(1, session.id);
                    if (slave != null) {
                        st.setLong(2, slave.id);
                    }
                }
                
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        Node n = new Node();
                        transform(rs, n);
                        ret.add(n);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        return ret;
    }
    
    public Node getNode(Long id) {
        Node n = null;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return n;
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_NODE)) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    n = new Node();
                    transform(rs, n);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            n = null;
        } finally {
            releaseConnection(conn);
        }
        
        return n;
    }
    
    public void updateNode(Node n, String address) {
        if (n.id == null) return;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(UPDATE_NODE_ADDRESS)) {
            if (address == null) {
                st.setNull(1, Types.VARCHAR);
            } else {
                st.setString(1, address);
            }
            
            st.setLong(2, n.id);
            
            // execute the query
            st.executeUpdate();
            
            // broadcast node state changed event
            n.address = address;
            
            MasterServer.fireNodeStateChanged(n.sessionId, n);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public void updateNode(Node n, Node.State s) {
        if (n.id == null) return;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(UPDATE_NODE_STATE)) {
            st.setString(1, s.toString());
            st.setLong(2, n.id);
            
            // execute the query
            st.executeUpdate();
            
            // broadcast node state changed event
            n.state = s;

            MasterServer.fireNodeStateChanged(n.sessionId, n);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public void updateNode(Node n) {
        if (n.id == null) return;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(UPDATE_NODE)) {
            if (n.slaveId == null) {
                st.setNull(1, Types.INTEGER);
            } else {
                st.setLong(1, n.slaveId);
            }
            
            if (n.name == null) {
                st.setString(2, "n" + n.id.toString());
            } else {
                st.setString(2, n.name);
            }
            
            st.setLong(3, n.id);
            
            // execute the query
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public void assignNode(Node n, Slave s) {
        if (n.id == null) return;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(UPDATE_NODE_ASSIGNMENT)) {
            if ((s == null) || (s.id == null)) {
                st.setNull(1, Types.INTEGER);
            } else {
                st.setLong(1, s.id);
            }
            
            st.setLong(2, n.id);
            
            // execute the query
            st.executeUpdate();
            
            // set the assigned slave id field in the node object
            n.assignedSlaveId = s.id;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public void clearAssignment(Session s) {
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        String query = null;
        
        if (s == null) {
            // clear all assignments
            query = UPDATE_NODES_CLEAR;
        } else {
            // clear all assignments of a session
            query = UPDATE_NODES_CLEAR_SESSION;
        }
        
        try (PreparedStatement st = conn.prepareStatement(query)) {
            // set session id
            if (s != null) st.setLong(1, s.id);
            
            // execute the query
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public void resetSessions() {
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (Statement st = conn.createStatement()) {
            // reset all sessions
            st.executeUpdate(UPDATE_SESSIONS_RESET);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public void removeNode(Node n) {
        if (n.id == null) return;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(DELETE_NODE)) {
            // set the session id
            st.setLong(1, n.id);
            
            // execute insertion
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public Node createNode(Long sessionId, Long slaveId) {
        Long nodeId = null;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return null;

        try {
            // disable auto-commit
            conn.setAutoCommit(false);
            
            try (PreparedStatement st = conn.prepareStatement(INSERT_NODE, Statement.RETURN_GENERATED_KEYS)) {
                // set the name
                st.setLong(1, sessionId);
                
                // set the slave
                if (slaveId == null) {
                    st.setNull(2, Types.INTEGER);
                } else {
                    st.setLong(2, slaveId);
                }
                
                // execute insertion
                st.execute();
                
                // get session id from result-set
                try (ResultSet rs = st.getGeneratedKeys()) {
                    if (rs.next()) {
                        nodeId = rs.getLong(1);
                    } else {
                        // something went wrong
                        return null;
                    }
                }
            }
            
            // create initial node name
            try (PreparedStatement st = conn.prepareStatement(UPDATE_NODE_NAME)) {
                // set the node name
                st.setString(1, "n" + nodeId.toString());
                
                // set the node id
                st.setLong(2, nodeId);
                
                // execute insertion
                st.executeUpdate();
            }
            
            // commit changes
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            return null;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            releaseConnection(conn);
        }
        
        return getNode(nodeId);
    }
    
    public ArrayList<Slave> getSlaves() {
        ArrayList<Slave> ret = new ArrayList<Slave>();
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return ret;
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_SLAVES)) {
            // TODO: set right user id
            st.setLong(1, 1);
            
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Slave s = new Slave(rs.getLong(1));
                    transform(rs, s);
                    ret.add(s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        return ret;
    }
    
    public Slave getSlave(Long id) {
        Slave s = null;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return s;
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_SLAVE)) {
            st.setLong(1, id);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    s = new Slave(rs.getLong(1));
                    transform(rs, s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            s = null;
        } finally {
            releaseConnection(conn);
        }
        
        return s;
    }
    
    public Slave getSlave(String name, String address) {
        Slave s = null;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return s;
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_SLAVE_NAME_ADDRESS)) {
            st.setString(1, name);
            st.setString(2, address);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    s = new Slave(rs.getLong(1));
                    transform(rs, s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            s = null;
        } finally {
            releaseConnection(conn);
        }
        
        return s;
    }
    
    public void updateSlave(Slave s, Slave.State state) {
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(UPDATE_SLAVE_STATE)) {
            st.setString(1, state.toString());
            st.setLong(2, s.id);
            
            // execute the query
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        // update slave object
        s.state = state;
        
        MasterServer.fireSlaveStateChanged(s);
    }
    
    public void updateSlave(Slave s, Long owner, Long capacity) {
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(UPDATE_SLAVE_OWNER_CAPACITY)) {
            // set the owner
            if (owner == null) {
                st.setNull(1, Types.INTEGER);
            } else {
                st.setLong(1, owner);
            }
            
            // set the capacity
            if (capacity == null) {
                st.setNull(2, Types.INTEGER);
            } else {
                st.setLong(2, capacity);
            }
            
            st.setLong(3, s.id);
            
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public Slave createSlave(String name, String address, Long owner, Long capacity) {
        Long slaveId = null;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return null;

        try (PreparedStatement st = conn.prepareStatement(INSERT_SLAVE, Statement.RETURN_GENERATED_KEYS)) {
            // set the name
            st.setString(1, name);
            
            // set the address
            st.setString(2, address);
            
            // set the owner
            if (owner == null) {
                st.setNull(3, Types.INTEGER);
            } else {
                st.setLong(3, owner);
            }
            
            // set the capacity
            if (capacity == null) {
                st.setNull(4, Types.INTEGER);
            } else {
                st.setLong(4, capacity);
            }
            
            // execute insertion
            st.execute();
            
            // get session id from result-set
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) {
                    slaveId = rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        // something went wrong
        if (slaveId == null)
            return null;
        
        return getSlave(slaveId);
    }
    
    public void resetSlaves() {
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (Statement st = conn.createStatement()) {
            // execute the query
            st.executeUpdate(UPDATE_SLAVES_RESET);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public ArrayList<Session> getSessions() {
        ArrayList<Session> ret = new ArrayList<Session>();
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return ret;
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_SESSIONS)) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Session s = new Session(rs.getLong(1));
                    transform(rs, s);
                    ret.add(s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        return ret;
    }
    
    public Session getSession(Long id) {
        Session s = null;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return s;
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_SESSION)) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    s = new Session(rs.getLong(1));
                    transform(rs, s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        return s;
    }
    
    public Session createSession(String username) {
        Long sessionId = null;

        User user = getUser(username);

        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return null;

        try (PreparedStatement st = conn.prepareStatement(INSERT_SESSION, Statement.RETURN_GENERATED_KEYS)) {
            //set right user id
            st.setLong(1, user.id);
            
            // execute insertion
            st.execute();
            
            // get session id from result-set
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) {
                    sessionId = rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        // something went wrong
        if (sessionId == null)
            return null;
        
        Session s = getSession(sessionId);
        
        // broadcast session added
        MasterServer.fireSessionAdded(s);
        
        return s;
    }
    
    public void removeSession(final Session s) {
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try {
            // disable auto-commit
            conn.setAutoCommit(false);
            
            // remove all nodes of this session
            try (PreparedStatement st = conn.prepareStatement(DELETE_SESSION_NODES)) {
                // set the session id
                st.setLong(1, s.id);
                
                // execute insertion
                st.execute();
            }
        
            // remove the session
            try (PreparedStatement st = conn.prepareStatement(DELETE_SESSION)) {
                // set the session id
                st.setLong(1, s.id);
                
                // execute insertion
                st.execute();
            }
            
            // commit changes
            conn.commit();
            
            // enqueue directory removal
            MasterServer.invoke(new Task() {
                @Override
                public void run() {
                    try {
                        SessionContainer sc = SessionContainer.getContainer(s);
                        
                        // trigger initialization of the session
                        sc.initialize(null);

                        // destroy the container (remove all files)
                        sc.destroy();
                    } catch (IOException e) {
                        // ignore any errors
                        e.printStackTrace();
                    }
                }
            });
            
            // broadcast session removal
            MasterServer.fireSessionRemoved(s);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.setAutoCommit(false);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            releaseConnection(conn);
        }
    }
    
    public void updateSession(Session s) {
        // do not update session if name is not set
        if (s.name == null) return;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(UPDATE_SESSION_NAME)) {
            st.setString(1, s.name);
            st.setLong(2, s.id);
            
            // execute the query
            st.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public User getUser(Long userid) {
        User ret = null;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return ret;
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_USER)) {
            st.setLong(1, userid);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    ret = new User(rs.getLong(1));
                    ret.name = rs.getString(2);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        return ret;
    }
    
    public User getUser(String username) {
        User ret = null;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return ret;
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_USER_BY_NAME)) {
            st.setString(1, username);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    ret = new User(rs.getLong(1));
                    ret.name = rs.getString(2);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        return ret;
    }
    
    public void setState(Session s, Session.State state) {
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        String query = null;
        
        if (Session.State.ABORTED.equals(state) || Session.State.CANCELLED.equals(state)) {
            query = UPDATE_SESSION_STATE_ABORTED;
        }
        else if (Session.State.RUNNING.equals(state)) {
            query = UPDATE_SESSION_STATE_RUNNING;
        }
        else if (Session.State.FINISHED.equals(state)) {
            query = UPDATE_SESSION_STATE_FINISHED;
        }
        else if (Session.State.DRAFT.equals(state)) {
            query = UPDATE_SESSION_STATE_DRAFT;
        }
        else {
            query = UPDATE_SESSION_STATE;
        }
        
        try (PreparedStatement st = conn.prepareStatement(query)) {
            st.setString(1, state.toString());
            st.setLong(2, s.id);
            
            // execute the query
            st.executeUpdate();

            // broadcast session change
            s.state = state;
            MasterServer.fireSessionStateChanged(s);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public Set<String> getAddressAllocation() {
        Set<String> ret = new HashSet<String>();
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return ret;

        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(QUERY_ADDRESS_ALLOCS)) {
            while (rs.next()) {
                ret.add(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        return ret;
    }
    
    public List<SlaveAllocation> getSlaveAllocation() {
        // TODO: add user as filter to restricted slaves
        
        List<SlaveAllocation> ret = new LinkedList<SlaveAllocation>();
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return ret;
        
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(QUERY_SLAVE_ALLOCS)) {
            while (rs.next()) {
                SlaveAllocation sa = new SlaveAllocation(rs.getLong(1));
                
                sa.capacity = rs.getLong(2);
                
                // check if nodes.assigned_slave is null
                rs.getLong(4);
                if (rs.wasNull()) {
                    // if it is null, then no node is allocated to this slave
                    sa.allocation = 0L;
                } else {
                    // if it is not null, then the value (3) is the number of allocations
                    sa.allocation = rs.getLong(3);
                }

                ret.add(sa);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        return ret;
    }
    
    /**
     * store stats of one node int the database
     * @param n
     * @param data
     */
    public void putStats(Node n, String data) {
        // do not store 'null' data
        if (data == null) return;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(INSERT_STATS_DATA)) {
            // set session id
            st.setLong(1, n.sessionId);
            
            // set node id
            st.setLong(2, n.id);
            
            // set data
            st.setString(3, data);
            
            // execute insertion
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    /**
     * store stats of multiple nodes in the database
     * @param n
     * @param data
     */
    public void putStats(Session s, String data) {
        // do not store 'null' data
        if (data == null) return;
        
        // split json data into one data-set per node
        HashMap<Long, String> data_map = JsonStats.splitAll(data);
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(INSERT_STATS_DATA)) {
            // disable auto-commit
            conn.setAutoCommit(false);
            
            for (Entry<Long, String> e : data_map.entrySet()) {
                // set session id
                st.setLong(1, s.id);
                
                // set node id
                st.setLong(2, e.getKey());
                
                // set data
                st.setString(3, e.getValue());
                
                // execute insertion
                st.execute();
            }
            
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            releaseConnection(conn);
        }
    }
    
    public void dumpStats(Session session, OutputStream out) throws IOException {
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_STATS)) {
            // set session id
            st.setLong(1, session.id);
            
            long currentTimestamp = -1;
            
            out.write("{".getBytes());
            
            // execute query
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    if (currentTimestamp < rs.getTimestamp(1).getTime()) {
                        // close timestamp array if not initial
                        if (currentTimestamp >= 0) {
                            out.write("},".getBytes());
                        }
                        
                        // switch to next timestamp
                        currentTimestamp = rs.getTimestamp(1).getTime();
                        
                        // open timestamp array
                        out.write(("\"" + currentTimestamp + "\":{").getBytes());
                    } else {
                        out.write(",".getBytes());
                    }
                    
                    // write node data
                    out.write(("\"" + rs.getLong(2) + "\": " + rs.getString(3)).getBytes());
                }
            }
            
            // close timestamp array if not initial
            if (currentTimestamp >= 0) {
                out.write("}".getBytes());
            }
            
            out.write("}".getBytes());
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    private ArrayList<DataPoint> getAverageStats(Session s, Date begin, Date end) {
        ArrayList<DataPoint> ret = new ArrayList<DataPoint>();
        
        // TODO: calculate average stats
        
        return ret;
    }
    
    /**
     * Get statistical data
     * @param s The session the data belong to.
     * @param n The node the data belong to. May be null.
     * @param begin The begin of the selection range. May be null.
     * @param end The end of the selection range. May be null.
     * @return An hash-map of the JSON encoded data indexed by the node-id and timestamp.
     */
    public ArrayList<DataPoint> getStats(Session s, Node n, Date begin, Date end) {
        // return average stats if no node is given
        if (n == null) return getAverageStats(s, begin, end);
        
        ArrayList<DataPoint> ret = new ArrayList<DataPoint>();
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return ret;
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_STATS_BY_NODE)) {
            // set session id
            st.setLong(1, s.id);
            
            // set node id
            st.setLong(2, n.id);
            
            if (begin != null) {
                st.setTimestamp(3, new Timestamp(begin.getTime()));
            } else {
                st.setInt(3, 0);
            }
            
            if (end == null) end = new Date();
            st.setTimestamp(4, new Timestamp(end.getTime()));
            
            // execute query
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    DataPoint dp = new DataPoint();
                    dp.node = n.id;
                    dp.time = rs.getTimestamp(1);
                    dp.json = rs.getString(2);
                    
                    // put data into the data-set
                    ret.add(dp);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        // reverse ordering of data
        Collections.reverse(ret);
        
        return ret;
    }
    
    /**
     * Get the data records of a specific date
     * @param s The session the data belong to.
     * @param date Date of Dataset
     * @return An hash-map of the JSON encoded data indexed by the node-id.
     */
    public HashMap<Long, DataPoint> getStatsOf(Session s, Date date) {
        HashMap<Long, DataPoint> ret = new HashMap<Long, DataPoint>();
        
        // return an empty hash-map if session is not set
        if (s == null) return ret;
        
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return ret;
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_STATS_OF)) {
            // set session id
            //st.setDate(1, new java.sql.Date(date.getTime()));
            st.setTimestamp(1, new Timestamp(date.getTime()));
            st.setLong(2, s.id);
            
            // execute query
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    DataPoint dp = new DataPoint();
                    
                    dp.node = rs.getLong(1);
                    dp.time = rs.getTimestamp(2);
                    dp.json = rs.getString(3);

                    // put data into the data-set
                    ret.put(dp.node, dp);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        return ret;
    }
    
    public ArrayList<Date> getStatDates(Session s)
    {
        ArrayList<Date> ret = new ArrayList<Date>();
        Connection conn = getConnection();
        
        if ( conn == null) return ret;
        if ( s == null )
        {
            System.out.println("Database.getStatDates(...): WARNING: session is null!");
            return ret;
        }
        
        try (PreparedStatement st = conn.prepareStatement(QUERY_STATS_TIMESTAMPS)) {
            // set session id
            st.setLong(1, s.id);
            
            // execute query
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    ret.add(rs.getTimestamp(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        
        return ret;
    }

    /**
     * returns Credentials Object belong to sessionID
     * @param sessionId the session-id
     * @return valid Credentials Object, or null if not-existent
     */
    public Credentials getUserSession(String sessionId)
    {

        Credentials rCreds = null;

        Connection conn = getConnection();
        if (conn == null) return null;

        try (PreparedStatement st = conn.prepareStatement(QUERY_USERSESSION)) {

            st.setString(1, sessionId);
            ResultSet rs = st.executeQuery();
            if (rs.next())
            {
                    rCreds = new Credentials();
                    rCreds.setUsername(rs.getString(1));
                    rCreds.setSessionId(rs.getString(2));
                    rCreds.setSessionExpires(rs.getTimestamp(3).getTime());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
        return rCreds;
    }

    public boolean isUserSessionValid(Credentials checkCreds)
    {
        if ( checkCreds == null )
            return false;

        String sessionId = checkCreds.getSessionId();

        if ( sessionId == null )
            return false;

        Credentials creds = getUserSession(sessionId);

        if ( creds == null )
            return false;

        return creds.equals(checkCreds);
    }

    public void putUserSession(Credentials creds)
    {
        // get a connection from the pool
        Connection conn = getConnection();
        if (conn == null) return;

        try (PreparedStatement st = conn.prepareStatement(INSERT_USERSESSION)) {

            st.setString(1, creds.getUsername());
            st.setString(2, creds.getSessionId());
            st.setTimestamp(3, new Timestamp(creds.getSessionExpires()));

            st.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }

    }
    
    public void removeUserSession(String sid)
    {
        Connection conn = getConnection();
        if (conn == null) return;

        try (PreparedStatement st = conn.prepareStatement(DELETE_USERSESSION)) {

            st.setString(1, sid);
            st.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }

    public void putUser(String username)
    {
        Connection conn = getConnection();
        if (conn == null) return;

        try (PreparedStatement st = conn.prepareStatement(INSERT_USER)) {

            st.setString(1, username);
            st.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
    
    public void purgeUserSessions(){
        Connection conn = getConnection();
        if (conn == null) return;
        
        try (PreparedStatement st = conn.prepareStatement(PURGE_USERSESSION)) {

            st.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }
}
