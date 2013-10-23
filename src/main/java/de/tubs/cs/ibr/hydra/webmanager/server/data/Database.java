
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
import java.util.Properties;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.tubs.cs.ibr.hydra.webmanager.server.MasterServer;
import de.tubs.cs.ibr.hydra.webmanager.server.Task;
import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;
import de.tubs.cs.ibr.hydra.webmanager.shared.User;

public class Database {

    private static Database __db__ = new Database();
    private Connection mConn = null;
    
    private JSONParser mParser = new JSONParser();

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
        } catch (SQLException e) {
            mConn = null;
            System.err.println("Mysql Connection Error: ");
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
    
    public ArrayList<Node> getNodes(Session session) {
        return getNodes(session, null);
    }
    
    public ArrayList<Node> getNodes(Session session, Slave slave) {
        ArrayList<Node> ret = new ArrayList<Node>();
        
        final String fields = "id, slave, session, name, state, address, assigned_slave";
        
        try {
            PreparedStatement st;
            
            if (session == null) {
                st = mConn.prepareStatement("SELECT " + fields + " FROM nodes;");
            }
            else if (slave == null) {
                st = mConn.prepareStatement("SELECT " + fields + " FROM nodes WHERE session = ?;");
                st.setLong(1, session.id);
            }
            else {
                st = mConn.prepareStatement("SELECT " + fields + " FROM nodes WHERE session = ? AND slave = ?;");
                st.setLong(1, session.id);
                st.setLong(2, slave.id);
            }
            
            ResultSet rs = st.executeQuery();
            
            while (rs.next()) {
                Node n = new Node();
                
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
                
                ret.add(n);
            }
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public Node getNode(Long id) {
        Node n = null;
        
        try {
            PreparedStatement st = mConn.prepareStatement("SELECT id, slave, session, name, state, address, assigned_slave FROM nodes WHERE id = ?;");
            st.setLong(1, id);
            ResultSet rs = st.executeQuery();
            
            if (rs.next()) {
                n = new Node();
                
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
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        
        return n;
    }
    
    public void updateNode(Node n, String address) {
        if (n.id == null) return;
        
        try {
            PreparedStatement st = mConn.prepareStatement("UPDATE nodes SET `address` = ? WHERE id = ?;");
            
            if (address == null) {
                st.setNull(1, Types.VARCHAR);
            } else {
                st.setString(1, address);
            }
            
            st.setLong(2, n.id);
            
            // execute the query
            st.execute();
            
            // broadcast node state changed event
            n.address = address;
            MasterServer.fireNodeStateChanged(n.sessionId, n);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateNode(Node n, Node.State s) {
        if (n.id == null) return;
        
        try {
            PreparedStatement st = mConn.prepareStatement("UPDATE nodes SET `state` = ? WHERE id = ?;");
            
            st.setString(1, s.toString());
            st.setLong(2, n.id);
            
            // execute the query
            st.execute();
            
            // broadcast node state changed event
            n.state = s;
            MasterServer.fireNodeStateChanged(n.sessionId, n);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updateNode(Node n) {
        if (n.id == null) return;
        
        try {
            PreparedStatement st = mConn.prepareStatement("UPDATE nodes SET `slave` = ?, `name` = ? WHERE id = ?;");
            
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
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void assignNode(Node n, Slave s) {
        if (n.id == null) return;
        
        try {
            PreparedStatement st = mConn.prepareStatement("UPDATE nodes SET `assigned_slave` = ? WHERE id = ?;");
            
            if ((s == null) || (s.id == null)) {
                st.setNull(1, Types.INTEGER);
            } else {
                st.setLong(1, s.id);
            }
            
            st.setLong(2, n.id);
            
            // execute the query
            st.execute();
            
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
                st = mConn.prepareStatement("UPDATE nodes SET `assigned_slave` = NULL, `address` = NULL, `state` = 'draft';");
            } else {
                // clear all assignments of a session
                st = mConn.prepareStatement("UPDATE nodes SET `assigned_slave` = NULL, `address` = NULL, `state` = 'draft' WHERE session = ?;");
                
                // set session id
                st.setLong(1, s.id);
            }
            
            // execute the query
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void resetSessions() {
        try {
            // reset all sessions to 'draft' state
            PreparedStatement st = mConn.prepareStatement("UPDATE sessions SET `state` = 'draft', `started` = NULL, `aborted` = NULL, `finished` = NULL WHERE `state` != 'finished' AND `state` != 'error' AND `state` != 'aborted'");
            
            // execute the query
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void removeNode(Node n) {
        if (n.id == null) return;
        
        try {
            PreparedStatement st = mConn.prepareStatement("DELETE FROM nodes WHERE id = ?;");
            
            // set the session id
            st.setLong(1, n.id);
            
            // execute insertion
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Node createNode(Long sessionId, Long slaveId) {
        Long nodeId = null;

        try {
            PreparedStatement st = mConn.prepareStatement("INSERT INTO nodes (`session`, `slave`) VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS);

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
            ResultSet rs = st.getGeneratedKeys();
            if (rs.next()) {
                nodeId = rs.getLong(1);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // something went wrong
        if (nodeId == null)
            return null;
        
        // create initial node name
        try {
            PreparedStatement st = mConn.prepareStatement("UPDATE nodes SET `name` = ? WHERE id = ?;");

            // set the node name
            st.setString(1, "n" + nodeId.toString());
            
            // set the node id
            st.setLong(2, nodeId);
            
            // execute insertion
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return getNode(nodeId);
    }
    
    public ArrayList<Slave> getSlaves() {
        ArrayList<Slave> ret = new ArrayList<Slave>();
        
        try {
            PreparedStatement st = mConn.prepareStatement("SELECT id, name, address, state, owner, capacity FROM slaves WHERE owner IS NULL OR owner = ?;");
            
            // TODO: set right user id
            st.setLong(1, 1);
            
            ResultSet rs = st.executeQuery();
            
            while (rs.next()) {
                Slave s = new Slave(rs.getLong(1));
                
                s.name = rs.getString(2);
                s.address = rs.getString(3);
                if (rs.wasNull()) s.address = null;
                
                s.state = Slave.State.fromString(rs.getString(4));
                
                s.owner = rs.getLong(5);
                if (rs.wasNull()) s.owner = null;
                
                s.capacity = rs.getLong(6);
                if (rs.wasNull()) s.capacity = null;
                
                ret.add(s);
            }
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public Slave getSlave(Long id) {
        Slave s = null;
        
        try {
            PreparedStatement st = mConn.prepareStatement("SELECT id, name, address, state, owner, capacity FROM slaves WHERE id = ? LIMIT 0,1;");
            st.setLong(1, id);
            
            ResultSet rs = st.executeQuery();
            
            if (rs.next()) {
                s = new Slave(rs.getLong(1));
                s.name = rs.getString(2);
                
                s.address = rs.getString(3);
                if (rs.wasNull()) s.address = null;
                
                s.state = Slave.State.fromString(rs.getString(4));
                
                s.owner = rs.getLong(5);
                if (rs.wasNull()) s.owner = null;
                
                s.capacity = rs.getLong(6);
                if (rs.wasNull()) s.capacity = null;
            }
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        
        return s;
    }
    
    public Slave getSlave(String name, String address) {
        Slave s = null;
        
        try {
            PreparedStatement st = mConn.prepareStatement("SELECT id, name, address, state, owner, capacity FROM slaves WHERE name = ? AND address = ? LIMIT 0,1;");
            st.setString(1, name);
            st.setString(2, address);
            
            ResultSet rs = st.executeQuery();
            
            if (rs.next()) {
                s = new Slave(rs.getLong(1));
                s.name = rs.getString(2);
                s.address = rs.getString(3);
                if (rs.wasNull()) s.address = null;
                
                s.state = Slave.State.fromString(rs.getString(4));
                
                s.owner = rs.getLong(5);
                if (rs.wasNull()) s.owner = null;
                
                s.capacity = rs.getLong(6);
                if (rs.wasNull()) s.capacity = null;
            }
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        
        return s;
    }
    
    public void updateSlave(Slave s, Slave.State state) {
        try {
            PreparedStatement st = mConn.prepareStatement("UPDATE slaves SET `state` = ? WHERE id = ?;");
            
            st.setString(1, state.toString());
            st.setLong(2, s.id);
            
            // execute the query
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // update slave object
        s.state = state;
        
        MasterServer.fireSlaveStateChanged(s);
    }
    
    public void updateSlave(Slave s, Long owner, Long capacity) {
        try {
            PreparedStatement st = mConn.prepareStatement("UPDATE slaves SET `owner` = ?, `capacity` = ? WHERE id = ?;");
            
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
            
            // execute the query
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Slave createSlave(String name, String address, Long owner, Long capacity) {
        Long slaveId = null;

        try {
            PreparedStatement st = mConn.prepareStatement("INSERT INTO slaves (`name`, `address`, `owner`, `capacity`) VALUES (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);

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
            ResultSet rs = st.getGeneratedKeys();
            if (rs.next()) {
                slaveId = rs.getLong(1);
            }
            rs.close();
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
            PreparedStatement st = mConn.prepareStatement("UPDATE slaves SET `state` = 'disconnected';");

            // execute the query
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public ArrayList<Session> getSessions() {
        ArrayList<Session> ret = new ArrayList<Session>();
        
        try {
            PreparedStatement st = mConn.prepareStatement("SELECT sessions.id, sessions.user, users.name, sessions.name, sessions.created, sessions.started, sessions.aborted, sessions.finished, sessions.state FROM sessions LEFT JOIN users ON (users.id = sessions.user);");
            ResultSet rs = st.executeQuery();
            
            while (rs.next()) {
                Session s = new Session(rs.getLong(1));
                
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
                
                ret.add(s);
            }
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public Session getSession(Long id) {
        try {
            PreparedStatement st = mConn.prepareStatement("SELECT sessions.id, sessions.user, users.name, sessions.name, sessions.created, sessions.started, sessions.aborted, sessions.finished, sessions.state FROM sessions LEFT JOIN users ON (users.id = sessions.user) WHERE sessions.id = ?;");
            st.setLong(1, id);
            ResultSet rs = st.executeQuery();
            
            Session s = null;
            
            if (rs.next()) {
                s = new Session(rs.getLong(1));

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
            
            rs.close();
            
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public Session createSession() {
        Long sessionId = null;

        try {
            PreparedStatement st = mConn.prepareStatement("INSERT INTO sessions (`user`, `created`) VALUES (?, NOW());", Statement.RETURN_GENERATED_KEYS);
            
            // TODO: set right user id
            st.setLong(1, 1);
            
            // execute insertion
            st.execute();
            
            // get session id from result-set
            ResultSet rs = st.getGeneratedKeys();
            if (rs.next()) {
                sessionId = rs.getLong(1);
            }
            rs.close();
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
            PreparedStatement st = mConn.prepareStatement("DELETE FROM nodes WHERE session = ?;");
            
            // set the session id
            st.setLong(1, s.id);
            
            // execute insertion
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // remove the session
        try {
            PreparedStatement st = mConn.prepareStatement("DELETE FROM sessions WHERE id = ?;");
            
            // set the session id
            st.setLong(1, s.id);
            
            // execute insertion
            st.execute();
            
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
            PreparedStatement st = mConn.prepareStatement("UPDATE sessions SET `name` = ? WHERE id = ?;");
            
            st.setString(1, s.name);
            st.setLong(2, s.id);
            
            // execute the query
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public User getUser(Long userid) {
        User ret = null;
        
        try {
            PreparedStatement st = mConn.prepareStatement("SELECT id, name FROM users WHERE id = ?;");
            st.setLong(1, userid);
            
            ResultSet rs = st.executeQuery();
            
            if (rs.next()) {
                ret = new User(rs.getLong(1));
                ret.name = rs.getString(2);
            }
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public User getUser(String username) {
        User ret = null;
        
        try {
            PreparedStatement st = mConn.prepareStatement("SELECT id, name FROM users WHERE name = ?;");
            st.setString(1, username);
            
            ResultSet rs = st.executeQuery();
            
            if (rs.next()) {
                ret = new User(rs.getLong(1));
                ret.name = rs.getString(2);
            }
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public void setState(Session s, Session.State state) {
        try {
            PreparedStatement st = null;
            
            if (Session.State.ABORTED.equals(state)) {
                st = mConn.prepareStatement("UPDATE sessions SET `state` = ?, aborted = NOW() WHERE id = ?;");
            }
            else if (Session.State.RUNNING.equals(state)) {
                st = mConn.prepareStatement("UPDATE sessions SET `state` = ?, started = NOW() WHERE id = ?;");
            }
            else if (Session.State.FINISHED.equals(state)) {
                st = mConn.prepareStatement("UPDATE sessions SET `state` = ?, finished = NOW() WHERE id = ?;");
            }
            else if (Session.State.DRAFT.equals(state)) {
                st = mConn.prepareStatement("UPDATE sessions SET `state` = ?, started = NULL, aborted = NULL, finished = NULL WHERE id = ?;");
            }
            else {
                st = mConn.prepareStatement("UPDATE sessions SET `state` = ? WHERE id = ?;");
            }
            
            st.setString(1, state.toString());
            st.setLong(2, s.id);
            
            // execute the query
            st.execute();

            // broadcast session change
            s.state = state;
            MasterServer.fireSessionStateChanged(s);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Set<String> getAddressAllocation() {
        Set<String> ret = new HashSet<String>();
        
        try {
            PreparedStatement st = mConn.prepareStatement("SELECT DISTINCT `address` FROM nodes WHERE `address` != NULL ORDER BY `address`;");
            
            ResultSet rs = st.executeQuery();
            
            while (rs.next()) {
                ret.add(rs.getString(1));
            }
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public List<SlaveAllocation> getSlaveAllocation() {
        final String query = "SELECT `slaves`.`id`, `slaves`.`capacity`, COUNT(`slaves`.`id`) as allocation, `nodes`.`assigned_slave` "+
                "FROM `slaves` " +
                "LEFT JOIN `nodes` ON (`slaves`.`id` = `nodes`.`assigned_slave`) " +
                "WHERE `slaves`.`state` != 'disconnected' " +
                "GROUP BY `slaves`.`id`;";
        
        // TODO: add user as filter to restricted slaves
        
        List<SlaveAllocation> ret = new LinkedList<SlaveAllocation>();
        
        try {
            PreparedStatement st = mConn.prepareStatement(query);
            
            ResultSet rs = st.executeQuery();
            
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
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public void putStats(Node n, String data) {
        // do not store 'null' data
        if (data == null) return;
        
        try {
            PreparedStatement st = mConn.prepareStatement("INSERT INTO stats (`session`, `node`, `data`) VALUES (?, ?, ?);");
            
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
        }
    }
    
    public void dumpStats(Session session, OutputStream out) throws IOException {
        try {
            PreparedStatement st = mConn.prepareStatement("SELECT `timestamp`, `data` FROM stats WHERE session = ?;");
            
            // set session id
            st.setLong(1, session.id);
            
            // execute query
            ResultSet rs = st.executeQuery();
            
            while (rs.next()) {
                String line = String.valueOf(rs.getLong(1)) + " " + rs.getString(2) + "\n";
                out.write(line.getBytes());
            }
            
            rs.close();
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
            String range = "";
            if (begin != null)  range += " AND (`timestamp` > ?)";
            if (end != null)    range += " AND (`timestamp` <= ?)";
            int param_offset = 2;
            
            PreparedStatement st = mConn.prepareStatement("SELECT `timestamp`, `data` FROM stats WHERE session = ? AND node = ?" + range + " ORDER BY timestamp DESC LIMIT 0,100;");
            
            // set session id
            st.setLong(param_offset, n.id);
            param_offset++;
            
            // set session id
            st.setLong(1, s.id);
            
            if (begin != null) {
                st.setTimestamp(param_offset, new Timestamp(begin.getTime()));
                param_offset++;
            }
            if (end != null) {
                st.setTimestamp(param_offset, new Timestamp(end.getTime()));
                param_offset++;
            }
            
            // execute query
            ResultSet rs = st.executeQuery();
            
            while (rs.next()) {
                DataPoint data = transformJsonData(rs.getTimestamp(1), rs.getString(2));

                // put data into the data-set
                ret.add(data);
            }
            
            rs.close();
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
            PreparedStatement st = mConn.prepareStatement("SELECT `id`, `s`.`timestamp`, `s`.`data` FROM `nodes` LEFT JOIN (SELECT `timestamp`, `node`, `data` FROM `stats` ORDER BY `timestamp` DESC) as s ON (`s`.`node` = `nodes`.`id`) WHERE `session` = ? GROUP BY `id` ORDER BY `id`;");
            
            // set session id
            st.setLong(1, s.id);
            
            // execute query
            ResultSet rs = st.executeQuery();

            while (rs.next()) {
                Long nodeId = rs.getLong(1);
                
                DataPoint data = transformJsonData(rs.getTimestamp(2), rs.getString(3));

                // put data into the data-set
                ret.put(nodeId, data);
            }
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    private DataPoint transformJsonData(Timestamp ts, String jsonData) {
        /**
         * Data example
         * {
         * "position": {"y": "0", "x": "0", "state": "0", "z": "0"}, 
         * "dtnd": {
         *   "info": {"Neighbors": "0", "Uptime": "38", "Storage-size": "0"}, 
         *   "bundles": {"Received": "0", "Generated": "0", "Stored": "0", "Transmitted": "0", "Aborted": "0", "Requeued": "0", "Expired": "0", "Queued": "0"}
         * }, 
         * "iface": {"lo": {"rx": "0", "tx": "0"}, "eth1": {"rx": "0", "tx": "2846"}, "eth0": {"rx": "7330", "tx": "2784"}}, 
         * "clock": {"Delay": "0.02614", "Timex-tick": "10000", "Timex-offset": "0", "Timex-freq": "0", "Offset": "1.07629"}
         * }
         */
        
        DataPoint ret = new DataPoint();
        
        ret.time = ts;
        
        // translate JSON to Object
        try {
            JSONObject obj = (JSONObject)mParser.parse(jsonData);
            
            // return if the json is invalid
            if (obj == null) return ret;
            
            JSONObject position = (JSONObject)obj.get("position");
            if (position != null) {
                double x = Double.valueOf((String)position.get("x"));
                double y = Double.valueOf((String)position.get("y"));
                double z = Double.valueOf((String)position.get("z"));
                ret.coord = new Coordinates(x, y, z);
            }
            
            JSONObject dtnd = (JSONObject)obj.get("dtnd");
            if (dtnd != null) {
                JSONObject dtnd_info = (JSONObject)dtnd.get("info");
                if (dtnd_info != null) {
                    ret.dtninfo.neighbors = Long.valueOf((String)dtnd_info.get("Neighbors"));
                    ret.dtninfo.uptime = Long.valueOf((String)dtnd_info.get("Uptime"));
                    ret.dtninfo.storage_size = Long.valueOf((String)dtnd_info.get("Storage-size"));
                }
                
                JSONObject bundles = (JSONObject)dtnd.get("bundles");
                if (bundles != null) {
                    ret.bundlestats.aborted = Long.valueOf((String)bundles.get("Aborted"));
                    ret.bundlestats.expired = Long.valueOf((String)bundles.get("Expired"));
                    ret.bundlestats.generated = Long.valueOf((String)bundles.get("Generated"));
                    ret.bundlestats.queued = Long.valueOf((String)bundles.get("Queued"));
                    ret.bundlestats.received = Long.valueOf((String)bundles.get("Received"));
                    ret.bundlestats.requeued = Long.valueOf((String)bundles.get("Requeued"));
                    ret.bundlestats.transmitted = Long.valueOf((String)bundles.get("Transmitted"));
                    ret.bundlestats.stored = Long.valueOf((String)bundles.get("Stored"));
                }
            }
            
            JSONObject clock = (JSONObject)obj.get("clock");
            if (clock != null) {
                ret.clock.delay = Double.valueOf((String)clock.get("Delay"));
                ret.clock.offset = Double.valueOf((String)clock.get("Offset"));
                ret.clock.timex_tick = Long.valueOf((String)clock.get("Timex-tick"));
                ret.clock.timex_offset = Long.valueOf((String)clock.get("Timex-offset"));
                ret.clock.timex_freq = Long.valueOf((String)clock.get("Timex-freq"));
            }
            
            JSONObject ifaces = (JSONObject)obj.get("iface");
            
            for (Object key : ifaces.keySet()) {
                JSONObject iface_data = (JSONObject)ifaces.get(key);
                
                DataPoint.InterfaceStats iface = new DataPoint.InterfaceStats();
                iface.name = (String)key;
                
                iface.rx = Long.valueOf((String)iface_data.get("rx"));
                iface.tx = Long.valueOf((String)iface_data.get("tx"));
                
                ret.ifaces.put(iface.name, iface);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
}
