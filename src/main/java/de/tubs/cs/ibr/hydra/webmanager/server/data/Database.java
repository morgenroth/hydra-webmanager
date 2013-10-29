
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
import java.util.logging.Logger;

import de.tubs.cs.ibr.hydra.webmanager.server.MasterServer;
import de.tubs.cs.ibr.hydra.webmanager.server.Task;
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
    
    private final static String QUERY_NODES = "SELECT " + NODES_FIELDS + " FROM nodes;";
    private final static String QUERY_NODES_SESSION = "SELECT " + NODES_FIELDS + " FROM nodes WHERE session = ?;";
    private final static String QUERY_NODES_SESSION_SLAVE = "SELECT " + NODES_FIELDS + " FROM nodes WHERE session = ? AND slave = ?;";
    
    private final static String QUERY_NODE = "SELECT " + NODES_FIELDS + " FROM nodes WHERE id = ?;";
    
    private final static String QUERY_SLAVES = "SELECT " + SLAVES_FIELDS + " FROM slaves WHERE owner IS NULL OR owner = ?;";
    private final static String QUERY_SLAVE = "SELECT " + SLAVES_FIELDS + " FROM slaves WHERE id = ? LIMIT 0,1;";
    private final static String QUERY_SLAVE_NAME_ADDRESS = "SELECT " + SLAVES_FIELDS + " FROM slaves WHERE name = ? AND address = ? LIMIT 0,1;";
    
    private final static String QUERY_SESSIONS = "SELECT " + SESSIONS_FIELDS + " FROM sessions LEFT JOIN users ON (users.id = sessions.user);";
    private final static String QUERY_SESSION = "SELECT " + SESSIONS_FIELDS + " FROM sessions LEFT JOIN users ON (users.id = sessions.user) WHERE sessions.id = ?;";
    
    private final static String QUERY_USER = "SELECT " + USERS_FIELDS + " FROM users WHERE id = ?;";
    private final static String QUERY_USER_BY_NAME = "SELECT " + USERS_FIELDS + " FROM users WHERE name = ?;";
    
    private final static String QUERY_ADDRESS_ALLOCS = "SELECT DISTINCT `address` FROM nodes WHERE `address` != NULL ORDER BY `address`;";
    private final static String QUERY_SLAVE_ALLOCS = "SELECT `slaves`.`id`, `slaves`.`capacity`, COUNT(`slaves`.`id`) as allocation, `nodes`.`assigned_slave` FROM `slaves` LEFT JOIN `nodes` ON (`slaves`.`id` = `nodes`.`assigned_slave`) WHERE `slaves`.`state` != 'disconnected' GROUP BY `slaves`.`id`;";
    
    private final static String QUERY_STATS = "SELECT `timestamp`, `data` FROM stats WHERE session = ?;";
    private final static String QUERY_STATS_BY_NODE = "SELECT `timestamp`, `data` FROM stats WHERE session = ? AND node = ? AND (`timestamp` > ?) AND (`timestamp` <= ?) ORDER BY timestamp DESC LIMIT 0,100;";
    private final static String QUERY_STATS_LATEST = "SELECT `id`, `s`.`timestamp`, `s`.`data` FROM `nodes` RIGHT JOIN (SELECT `timestamp`, `node`, `data` FROM `stats` WHERE `session` = ? ORDER BY `timestamp` DESC) as s ON (`s`.`node` = `nodes`.`id`) WHERE `session` = ? GROUP BY `id` ORDER BY `id`;";
    
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
    
    private final static String UPDATE_SESSIONS_RESET = "UPDATE sessions SET `state` = 'draft', `started` = NULL, `aborted` = NULL, `finished` = NULL WHERE `state` != 'finished' AND `state` != 'error' AND `state` != 'aborted'";
    
    private final static String DELETE_NODE = "DELETE FROM nodes WHERE id = ?;";
    private final static String DELETE_SESSION_NODES = "DELETE FROM nodes WHERE session = ?;";
    private final static String DELETE_SESSION = "DELETE FROM sessions WHERE id = ?;";
    
    private final static String INSERT_NODE = "INSERT INTO nodes (`session`, `slave`) VALUES (?, ?);";
    private final static String INSERT_SLAVE = "INSERT INTO slaves (`name`, `address`, `owner`, `capacity`) VALUES (?, ?, ?, ?);";
    private final static String INSERT_SESSION = "INSERT INTO sessions (`user`, `created`) VALUES (?, NOW());";
    private final static String INSERT_STATS_DATA = "INSERT INTO stats (`session`, `node`, `data`) VALUES (?, ?, ?);";
    
    private PreparedStatement STMT_QUERY_NODES = null;
    private PreparedStatement STMT_QUERY_NODES_SESSION = null;
    private PreparedStatement STMT_QUERY_NODES_SESSION_SLAVE = null;
    
    private PreparedStatement STMT_QUERY_NODE = null;
    
    private PreparedStatement STMT_QUERY_SLAVES = null;
    private PreparedStatement STMT_QUERY_SLAVE = null;
    private PreparedStatement STMT_QUERY_SLAVE_NAME_ADDRESS = null;
    
    private PreparedStatement STMT_QUERY_SESSIONS = null;
    private PreparedStatement STMT_QUERY_SESSION = null;
    
    private PreparedStatement STMT_QUERY_USER = null;
    private PreparedStatement STMT_QUERY_USER_BY_NAME = null;
    
    private PreparedStatement STMT_QUERY_ADDRESS_ALLOCS = null;
    private PreparedStatement STMT_QUERY_SLAVE_ALLOCS = null;
    
    private PreparedStatement STMT_QUERY_STATS = null;
    private PreparedStatement STMT_QUERY_STATS_BY_NODE = null;
    private PreparedStatement STMT_QUERY_STATS_LATEST = null;
    
    private PreparedStatement STMT_UPDATE_NODE_ADDRESS = null;
    private PreparedStatement STMT_UPDATE_NODE_STATE = null;
    private PreparedStatement STMT_UPDATE_NODE = null;
    private PreparedStatement STMT_UPDATE_NODE_ASSIGNMENT = null;
    private PreparedStatement STMT_UPDATE_NODE_NAME = null;
    
    private PreparedStatement STMT_UPDATE_NODES_CLEAR = null;
    private PreparedStatement STMT_UPDATE_NODES_CLEAR_SESSION = null;
    
    private PreparedStatement STMT_UPDATE_SLAVE_STATE = null;
    private PreparedStatement STMT_UPDATE_SLAVE_OWNER_CAPACITY = null;
    private PreparedStatement STMT_UPDATE_SLAVES_RESET = null;
    
    private PreparedStatement STMT_UPDATE_SESSION_NAME = null;
    private PreparedStatement STMT_UPDATE_SESSION_STATE_ABORTED = null;
    private PreparedStatement STMT_UPDATE_SESSION_STATE_RUNNING = null;
    private PreparedStatement STMT_UPDATE_SESSION_STATE_FINISHED = null;
    private PreparedStatement STMT_UPDATE_SESSION_STATE_DRAFT = null;
    private PreparedStatement STMT_UPDATE_SESSION_STATE = null;

    private PreparedStatement STMT_UPDATE_SESSIONS_RESET = null;
    
    private PreparedStatement STMT_DELETE_NODE = null;
    private PreparedStatement STMT_DELETE_SESSION_NODES = null;
    private PreparedStatement STMT_DELETE_SESSION = null;
    
    private PreparedStatement STMT_INSERT_NODE = null;
    private PreparedStatement STMT_INSERT_SLAVE = null;
    private PreparedStatement STMT_INSERT_SESSION = null;
    private PreparedStatement STMT_INSERT_STATS_DATA = null;
    
    private static Database __db__ = new Database();
    private Connection mConn = null;
    
    public static Database getInstance() {
        if (__db__.isClosed()) __db__.open();
        return __db__;
    }
    
    public void close() {
        if (mConn != null)
            try {
                mConn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }

    public boolean isClosed() {
        try {
            return (mConn == null) || mConn.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    private void open() {
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
            mConn = DriverManager.getConnection(url + dbname + "?autoReconnect=true", username, password);
            
            // prepare all statements
            prepareStatements();
        } catch (SQLException e) {
            mConn = null;
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
    }
    
    private void prepareStatements() throws SQLException {
         STMT_QUERY_NODES = mConn.prepareStatement(QUERY_NODES);
         STMT_QUERY_NODES_SESSION = mConn.prepareStatement(QUERY_NODES_SESSION);
         STMT_QUERY_NODES_SESSION_SLAVE = mConn.prepareStatement(QUERY_NODES_SESSION_SLAVE);
        
         STMT_QUERY_NODE = mConn.prepareStatement(QUERY_NODE);
        
         STMT_QUERY_SLAVES = mConn.prepareStatement(QUERY_SLAVES);
         STMT_QUERY_SLAVE = mConn.prepareStatement(QUERY_SLAVE);
         STMT_QUERY_SLAVE_NAME_ADDRESS = mConn.prepareStatement(QUERY_SLAVE_NAME_ADDRESS);
        
         STMT_QUERY_SESSIONS = mConn.prepareStatement(QUERY_SESSIONS);
         STMT_QUERY_SESSION = mConn.prepareStatement(QUERY_SESSION);
        
         STMT_QUERY_USER = mConn.prepareStatement(QUERY_USER);
         STMT_QUERY_USER_BY_NAME = mConn.prepareStatement(QUERY_USER_BY_NAME);
        
         STMT_QUERY_ADDRESS_ALLOCS = mConn.prepareStatement(QUERY_ADDRESS_ALLOCS);
         STMT_QUERY_SLAVE_ALLOCS = mConn.prepareStatement(QUERY_SLAVE_ALLOCS);
        
         STMT_QUERY_STATS = mConn.prepareStatement(QUERY_STATS);
         STMT_QUERY_STATS_BY_NODE = mConn.prepareStatement(QUERY_STATS_BY_NODE);
         STMT_QUERY_STATS_LATEST = mConn.prepareStatement(QUERY_STATS_LATEST);
        
         STMT_UPDATE_NODE_ADDRESS = mConn.prepareStatement(UPDATE_NODE_ADDRESS);
         STMT_UPDATE_NODE_STATE = mConn.prepareStatement(UPDATE_NODE_STATE);
         STMT_UPDATE_NODE = mConn.prepareStatement(UPDATE_NODE);
         STMT_UPDATE_NODE_ASSIGNMENT = mConn.prepareStatement(UPDATE_NODE_ASSIGNMENT);
         STMT_UPDATE_NODE_NAME = mConn.prepareStatement(UPDATE_NODE_NAME);
        
         STMT_UPDATE_SLAVE_STATE = mConn.prepareStatement(UPDATE_SLAVE_STATE);
         STMT_UPDATE_SLAVE_OWNER_CAPACITY = mConn.prepareStatement(UPDATE_SLAVE_OWNER_CAPACITY);
         STMT_UPDATE_SLAVES_RESET = mConn.prepareStatement(UPDATE_SLAVES_RESET);
        
         STMT_UPDATE_SESSION_NAME = mConn.prepareStatement(UPDATE_SESSION_NAME);
         STMT_UPDATE_SESSION_STATE_ABORTED = mConn.prepareStatement(UPDATE_SESSION_STATE_ABORTED);
         STMT_UPDATE_SESSION_STATE_RUNNING = mConn.prepareStatement(UPDATE_SESSION_STATE_RUNNING);
         STMT_UPDATE_SESSION_STATE_FINISHED = mConn.prepareStatement(UPDATE_SESSION_STATE_FINISHED);
         STMT_UPDATE_SESSION_STATE_DRAFT = mConn.prepareStatement(UPDATE_SESSION_STATE_DRAFT);
         STMT_UPDATE_SESSION_STATE = mConn.prepareStatement(UPDATE_SESSION_STATE);
        
         STMT_UPDATE_NODES_CLEAR = mConn.prepareStatement(UPDATE_NODES_CLEAR);
         STMT_UPDATE_NODES_CLEAR_SESSION = mConn.prepareStatement(UPDATE_NODES_CLEAR_SESSION);
        
         STMT_UPDATE_SESSIONS_RESET = mConn.prepareStatement(UPDATE_SESSIONS_RESET);
        
         STMT_DELETE_NODE = mConn.prepareStatement(DELETE_NODE);
         STMT_DELETE_SESSION_NODES = mConn.prepareStatement(DELETE_SESSION_NODES);
         STMT_DELETE_SESSION = mConn.prepareStatement(DELETE_SESSION);
        
         STMT_INSERT_NODE = mConn.prepareStatement(INSERT_NODE, Statement.RETURN_GENERATED_KEYS);
         STMT_INSERT_SLAVE = mConn.prepareStatement(INSERT_SLAVE, Statement.RETURN_GENERATED_KEYS);
         STMT_INSERT_SESSION = mConn.prepareStatement(INSERT_SESSION, Statement.RETURN_GENERATED_KEYS);
         STMT_INSERT_STATS_DATA = mConn.prepareStatement(INSERT_STATS_DATA);
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
        
        try {
            PreparedStatement st;
            
            if (session == null) {
                st = STMT_QUERY_NODES;
            }
            else if (slave == null) {
                st = STMT_QUERY_NODES_SESSION;
            }
            else {
                st = STMT_QUERY_NODES_SESSION_SLAVE;
            }
            
            synchronized(st) {
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
        }
        
        return ret;
    }
    
    public Node getNode(Long id) {
        Node n = null;
        
        try {
            PreparedStatement st = STMT_QUERY_NODE;
            
            synchronized(st) {
                st.setLong(1, id);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        n = new Node();
                        transform(rs, n);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        
        return n;
    }
    
    public void updateNode(Node n, String address) {
        if (n.id == null) return;
        
        try {
            PreparedStatement st = STMT_UPDATE_NODE_ADDRESS;
            
            synchronized(st) {
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
            }
            
            MasterServer.fireNodeStateChanged(n.sessionId, n);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateNode(Node n, Node.State s) {
        if (n.id == null) return;
        
        try {
            PreparedStatement st = STMT_UPDATE_NODE_STATE;
            
            synchronized(st) {
                st.setString(1, s.toString());
                st.setLong(2, n.id);
                
                // execute the query
                st.executeUpdate();
                
                // broadcast node state changed event
                n.state = s;
            }
            MasterServer.fireNodeStateChanged(n.sessionId, n);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateNode(Node n) {
        if (n.id == null) return;
        
        try {
            PreparedStatement st = STMT_UPDATE_NODE;
            
            synchronized(st) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void assignNode(Node n, Slave s) {
        if (n.id == null) return;
        
        try {
            PreparedStatement st = STMT_UPDATE_NODE_ASSIGNMENT;
            
            synchronized(st) {
                if ((s == null) || (s.id == null)) {
                    st.setNull(1, Types.INTEGER);
                } else {
                    st.setLong(1, s.id);
                }
                
                st.setLong(2, n.id);
                
                // execute the query
                st.executeUpdate();
            }
            
            // set the assigned slave id field in the node object
            n.assignedSlaveId = s.id;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void clearAssignment(Session s) {
        try {
            PreparedStatement st = null;
            
            if (s == null) {
                // clear all assignments
                st = STMT_UPDATE_NODES_CLEAR;
            } else {
                // clear all assignments of a session
                st = STMT_UPDATE_NODES_CLEAR_SESSION;
            }
            
            synchronized(st) {
                // set session id
                if (s != null) st.setLong(1, s.id);
                
                // execute the query
                st.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void resetSessions() {
        try {
            // reset all sessions to 'draft' state
            synchronized(STMT_UPDATE_SESSIONS_RESET) {
                // execute the query
                STMT_UPDATE_SESSIONS_RESET.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void removeNode(Node n) {
        if (n.id == null) return;
        
        try {
            PreparedStatement st = STMT_DELETE_NODE;
            
            synchronized(st) {
                // set the session id
                st.setLong(1, n.id);
                
                // execute insertion
                st.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Node createNode(Long sessionId, Long slaveId) {
        Long nodeId = null;

        try {
            PreparedStatement st = STMT_INSERT_NODE;
            
            synchronized(st) {
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
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // something went wrong
        if (nodeId == null)
            return null;
        
        // create initial node name
        try {
            PreparedStatement st = STMT_UPDATE_NODE_NAME;
            
            synchronized(st) {
                // set the node name
                st.setString(1, "n" + nodeId.toString());
                
                // set the node id
                st.setLong(2, nodeId);
                
                // execute insertion
                st.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return getNode(nodeId);
    }
    
    public ArrayList<Slave> getSlaves() {
        ArrayList<Slave> ret = new ArrayList<Slave>();
        
        try {
            PreparedStatement st = STMT_QUERY_SLAVES;
            
            synchronized(st) {
                // TODO: set right user id
                st.setLong(1, 1);
                
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        Slave s = new Slave(rs.getLong(1));
                        transform(rs, s);
                        ret.add(s);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public Slave getSlave(Long id) {
        Slave s = null;
        
        try {
            PreparedStatement st = STMT_QUERY_SLAVE;
            
            synchronized(st) {
                st.setLong(1, id);
                
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        s = new Slave(rs.getLong(1));
                        transform(rs, s);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        
        return s;
    }
    
    public Slave getSlave(String name, String address) {
        Slave s = null;
        
        try {
            PreparedStatement st = STMT_QUERY_SLAVE_NAME_ADDRESS;
            
            synchronized(st) {
                st.setString(1, name);
                st.setString(2, address);
                
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        s = new Slave(rs.getLong(1));
                        transform(rs, s);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        
        return s;
    }
    
    public void updateSlave(Slave s, Slave.State state) {
        try {
            PreparedStatement st = STMT_UPDATE_SLAVE_STATE;
            
            synchronized(st) {
                st.setString(1, state.toString());
                st.setLong(2, s.id);
                
                // execute the query
                st.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // update slave object
        s.state = state;
        
        MasterServer.fireSlaveStateChanged(s);
    }
    
    public void updateSlave(Slave s, Long owner, Long capacity) {
        try {
            PreparedStatement st = STMT_UPDATE_SLAVE_OWNER_CAPACITY;
            
            synchronized(st) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Slave createSlave(String name, String address, Long owner, Long capacity) {
        Long slaveId = null;

        try {
            PreparedStatement st = STMT_INSERT_SLAVE;

            synchronized(st) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // something went wrong
        if (slaveId == null)
            return null;
        
        return getSlave(slaveId);
    }
    
    public void resetSlaves() {
        try {
            synchronized(STMT_UPDATE_SLAVES_RESET) {
                // execute the query
                STMT_UPDATE_SLAVES_RESET.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public ArrayList<Session> getSessions() {
        ArrayList<Session> ret = new ArrayList<Session>();
        
        try {
            PreparedStatement st = STMT_QUERY_SESSIONS;
            
            synchronized(st) {
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        Session s = new Session(rs.getLong(1));
                        transform(rs, s);
                        ret.add(s);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public Session getSession(Long id) {
        Session s = null;
        
        try {
            PreparedStatement st = STMT_QUERY_SESSION;
            
            synchronized(st) {
                st.setLong(1, id);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        s = new Session(rs.getLong(1));
                        transform(rs, s);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return s;
    }
    
    public Session createSession() {
        Long sessionId = null;

        try {
            PreparedStatement st = STMT_INSERT_SESSION;
            
            synchronized(st) {
                // TODO: set right user id
                st.setLong(1, 1);
                
                // execute insertion
                st.execute();
                
                // get session id from result-set
                try (ResultSet rs = st.getGeneratedKeys()) {
                    if (rs.next()) {
                        sessionId = rs.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        // remove all nodes of this session
        try {
            PreparedStatement st = STMT_DELETE_SESSION_NODES;
            
            synchronized(st) {
                // set the session id
                st.setLong(1, s.id);
                
                // execute insertion
                st.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // remove the session
        try {
            PreparedStatement st = STMT_DELETE_SESSION;
            
            synchronized(st) {
                // set the session id
                st.setLong(1, s.id);
                
                // execute insertion
                st.execute();
            }
            
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
        }
    }
    
    public void updateSession(Session s) {
        // do not update session if name is not set
        if (s.name == null) return;
        
        try {
            PreparedStatement st = STMT_UPDATE_SESSION_NAME;
            
            synchronized(st) {
                st.setString(1, s.name);
                st.setLong(2, s.id);
                
                // execute the query
                st.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public User getUser(Long userid) {
        User ret = null;
        
        try {
            PreparedStatement st = STMT_QUERY_USER;
            
            synchronized(st) {
                st.setLong(1, userid);
                
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        ret = new User(rs.getLong(1));
                        ret.name = rs.getString(2);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public User getUser(String username) {
        User ret = null;
        
        try {
            PreparedStatement st = STMT_QUERY_USER_BY_NAME;
            
            synchronized(st) {
                st.setString(1, username);
                
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        ret = new User(rs.getLong(1));
                        ret.name = rs.getString(2);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public void setState(Session s, Session.State state) {
        try {
            PreparedStatement st = null;
            
            if (Session.State.ABORTED.equals(state)) {
                st = STMT_UPDATE_SESSION_STATE_ABORTED;
            }
            else if (Session.State.RUNNING.equals(state)) {
                st = STMT_UPDATE_SESSION_STATE_RUNNING;
            }
            else if (Session.State.FINISHED.equals(state)) {
                st = STMT_UPDATE_SESSION_STATE_FINISHED;
            }
            else if (Session.State.DRAFT.equals(state)) {
                st = STMT_UPDATE_SESSION_STATE_DRAFT;
            }
            else {
                st = STMT_UPDATE_SESSION_STATE;
            }
            
            synchronized(st) {
                st.setString(1, state.toString());
                st.setLong(2, s.id);
                
                // execute the query
                st.executeUpdate();
            }

            // broadcast session change
            s.state = state;
            MasterServer.fireSessionStateChanged(s);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Set<String> getAddressAllocation() {
        Set<String> ret = new HashSet<String>();
        
        PreparedStatement st = STMT_QUERY_ADDRESS_ALLOCS;
        
        synchronized(st) {
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    ret.add(rs.getString(1));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        return ret;
    }
    
    public List<SlaveAllocation> getSlaveAllocation() {
        // TODO: add user as filter to restricted slaves
        
        List<SlaveAllocation> ret = new LinkedList<SlaveAllocation>();
        
        try {
            PreparedStatement st = STMT_QUERY_SLAVE_ALLOCS;
            
            synchronized(st) {
                try (ResultSet rs = st.executeQuery()) {
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
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        
        try {
            PreparedStatement st = STMT_INSERT_STATS_DATA;
            
            synchronized(st) {
                // set session id
                st.setLong(1, n.sessionId);
                
                // set node id
                st.setLong(2, n.id);
                
                // set data
                st.setString(3, data);
                
                // execute insertion
                st.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        
        try {
            // split json data into one data-set per node
            HashMap<Long, String> data_map = JsonStats.splitAll(data);
            
            PreparedStatement st = STMT_INSERT_STATS_DATA;
            
            synchronized(st) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void dumpStats(Session session, OutputStream out) throws IOException {
        try {
            PreparedStatement st = STMT_QUERY_STATS;
            
            synchronized(st) {
                // set session id
                st.setLong(1, session.id);
                
                // execute query
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        String line = rs.getTimestamp(1).getTime() + " " + rs.getString(2) + "\n";
                        out.write(line.getBytes());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        
        try {
            PreparedStatement st = STMT_QUERY_STATS_BY_NODE;
            
            synchronized(st) {
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
                        DataPoint data = JsonStats.decode(rs.getTimestamp(1), rs.getString(2));
        
                        // put data into the data-set
                        ret.add(data);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // reverse ordering of data
        Collections.reverse(ret);
        
        return ret;
    }
    
    /**
     * Get the latest data records
     * @param The session the data belong to.
     * @return An hash-map of the JSON encoded data indexed by the node-id.
     */
    public HashMap<Long, DataPoint> getStatsLatest(Session s) {
        HashMap<Long, DataPoint> ret = new HashMap<Long, DataPoint>();
        
        // return an empty hash-map if session is not set
        if (s == null) return ret;
        
        try {
            PreparedStatement st = STMT_QUERY_STATS_LATEST;
            
            synchronized(st) {
                // set session id
                st.setLong(1, s.id);
                st.setLong(2, s.id);
                
                // execute query
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        DataPoint data = null;
                        
                        Long nodeId = rs.getLong(1);
                        Timestamp ts = rs.getTimestamp(2);
                        String json = rs.getString(3);
                        
                        if (rs.wasNull()) {
                            data = new DataPoint();
                        } else {
                            data = JsonStats.decode(ts, json);
                        }
        
                        // put data into the data-set
                        ret.put(nodeId, data);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
}
