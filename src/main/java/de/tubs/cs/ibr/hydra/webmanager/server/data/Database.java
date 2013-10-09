
package de.tubs.cs.ibr.hydra.webmanager.server.data;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.tubs.cs.ibr.hydra.webmanager.server.MasterServer;
import de.tubs.cs.ibr.hydra.webmanager.server.Task;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventExtra;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class Database {

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
    
    public ArrayList<Node> getNodes(String sessionKey) {
        ArrayList<Node> ret = new ArrayList<Node>();
        
        try {
            PreparedStatement st;
            
            if (sessionKey == null) {
                st = mConn.prepareStatement("SELECT id, slave, session, name, state, address FROM nodes;");
            } else {
                st = mConn.prepareStatement("SELECT id, slave, session, name, state, address FROM nodes WHERE session = ?;");
                st.setString(1, sessionKey);
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
            PreparedStatement st = mConn.prepareStatement("SELECT id, slave, session, name, state, address FROM nodes WHERE id = ?;");
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
            
            if (n.slaveId == null) {
                st.setNull(1, Types.INTEGER);
            } else {
                st.setLong(1, n.slaveId);
            }
            
            st.setLong(2, n.id);
            
            // execute the query
            st.execute();
            
            // broadcast node state changed event
            List<EventExtra> entries = new ArrayList<EventExtra>();
            entries.add(MasterServer.createEventExtra(EventType.EXTRA_NODE_ID, n.id.toString()));
            entries.add(MasterServer.createEventExtra(EventType.EXTRA_SESSION_ID, n.sessionId.toString()));
            
            MasterServer.broadcast(EventType.NODE_STATE_CHANGED, entries);
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
            List<EventExtra> entries = new ArrayList<EventExtra>();
            entries.add(MasterServer.createEventExtra(EventType.EXTRA_NODE_ID, n.id.toString()));
            entries.add(MasterServer.createEventExtra(EventType.EXTRA_SESSION_ID, n.sessionId.toString()));
            
            MasterServer.broadcast(EventType.NODE_STATE_CHANGED, entries);
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
            PreparedStatement st = mConn.prepareStatement("SELECT id, name, address, state, owner FROM slaves WHERE owner IS NULL OR owner = ?;");
            
            // TODO: set right user id
            st.setLong(1, 1);
            
            ResultSet rs = st.executeQuery();
            
            while (rs.next()) {
                Slave s = new Slave();
                
                s.id = rs.getLong(1);
                s.name = rs.getString(2);
                s.address = rs.getString(3);
                if (rs.wasNull()) s.address = null;
                
                s.state = Slave.State.fromString(rs.getString(4));
                
                s.owner = rs.getLong(5);
                if (rs.wasNull()) s.owner = null;
                
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
            PreparedStatement st = mConn.prepareStatement("SELECT id, name, address, state, owner FROM slaves WHERE id = ? LIMIT 0,1;");
            st.setLong(1, id);
            
            ResultSet rs = st.executeQuery();
            
            if (rs.next()) {
                s = new Slave();
                s.id = rs.getLong(1);
                s.name = rs.getString(2);
                
                s.address = rs.getString(3);
                if (rs.wasNull()) s.address = null;
                
                s.state = Slave.State.fromString(rs.getString(4));
                
                s.owner = rs.getLong(5);
                if (rs.wasNull()) s.owner = null;
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
            PreparedStatement st = mConn.prepareStatement("SELECT id, name, address, state, owner FROM slaves WHERE name = ? AND address = ? LIMIT 0,1;");
            st.setString(1, name);
            st.setString(2, address);
            
            ResultSet rs = st.executeQuery();
            
            if (rs.next()) {
                s = new Slave();
                s.id = rs.getLong(1);
                s.name = rs.getString(2);
                s.address = rs.getString(3);
                if (rs.wasNull()) s.address = null;
                
                s.state = Slave.State.fromString(rs.getString(4));
                
                s.owner = rs.getLong(5);
                if (rs.wasNull()) s.owner = null;
            }
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        
        return s;
    }
    
    public void updateSlave(Slave s) {
        try {
            PreparedStatement st = mConn.prepareStatement("UPDATE slaves SET `state` = ? WHERE id = ?;");
            
            st.setString(1, s.state.toString());
            st.setLong(2, s.id);
            
            // execute the query
            st.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Slave createSlave(String name, String address, Long owner) {
        Long slaveId = null;

        try {
            PreparedStatement st = mConn.prepareStatement("INSERT INTO slaves (`name`, `address`, `owner`) VALUES (?, ?, ?);", Statement.RETURN_GENERATED_KEYS);

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
                Session s = new Session();
                
                s.id = rs.getLong(1);
                s.userid = rs.getLong(2);
                s.username = rs.getString(3);
                
                s.name = rs.getString(4);
                if (rs.wasNull()) s.name = null;
                
                s.created = rs.getDate(5);
                
                s.started = rs.getDate(6);
                if (rs.wasNull()) s.started = null;
                
                s.aborted = rs.getDate(7);
                if (rs.wasNull()) s.aborted = null;
                
                s.finished = rs.getDate(8);
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
                s = new Session();

                s.id = rs.getLong(1);
                s.userid = rs.getLong(2);
                s.username = rs.getString(3);
                s.name = rs.getString(4);
                if (rs.wasNull()) s.name = null;
                
                s.created = rs.getDate(5);
                
                s.started = rs.getDate(6);
                if (rs.wasNull()) s.started = null;
                
                s.aborted = rs.getDate(7);
                if (rs.wasNull()) s.aborted = null;
                
                s.finished = rs.getDate(8);
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
        
        // broadcast session removal
        List<EventExtra> entries = new ArrayList<EventExtra>();
        entries.add(MasterServer.createEventExtra(EventType.EXTRA_SESSION_ID, sessionId.toString()));
        
        MasterServer.broadcast(EventType.SESSION_ADDED, entries);
        
        return getSession(sessionId);
    }
    
    public void removeSession(final Session s) {
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
            List<EventExtra> entries = new ArrayList<EventExtra>();
            entries.add(MasterServer.createEventExtra(EventType.EXTRA_SESSION_ID, s.id.toString()));
            
            MasterServer.broadcast(EventType.SESSION_REMOVED, entries);
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
    
    public String getUsername(Long userid) {
        String ret = null;
        
        try {
            PreparedStatement st = mConn.prepareStatement("SELECT name FROM users WHERE id = ?;");
            st.setLong(1, userid);
            
            ResultSet rs = st.executeQuery();
            
            if (rs.next()) {
                ret = rs.getString(1);
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
            else if (Session.State.PENDING.equals(state)) {
                st = mConn.prepareStatement("UPDATE sessions SET `state` = ?, started = NOW() WHERE id = ?;");
            }
            else if (Session.State.FINISHED.equals(state)) {
                st = mConn.prepareStatement("UPDATE sessions SET `state` = ?, finished = NOW() WHERE id = ?;");
            }
            else {
                st = mConn.prepareStatement("UPDATE sessions SET `state` = ? WHERE id = ?;");
            }
            
            st.setString(1, state.toString());
            st.setLong(2, s.id);
            
            // execute the query
            st.execute();
            
            List<EventExtra> entries = new ArrayList<EventExtra>();
            entries.add(MasterServer.createEventExtra(EventType.EXTRA_SESSION_ID, s.id.toString()));
            entries.add(MasterServer.createEventExtra(EventType.EXTRA_NEW_STATE, state.toString()));
            
            // broadcast session change
            MasterServer.broadcast(EventType.SESSION_STATE_CHANGED, entries);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
