
package de.tubs.cs.ibr.hydra.webmanager.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Date;

import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class Database {

    private static Database __db__ = new Database();
    private static String __url__ = "jdbc:mysql://localhost:3306/";
    private static String __dbname__ = "hydra";
    private static String __username__ = "hydra";
    private static String __password__ = "";

    private Connection mConn = null;

    public static Database getInstance() {
        if (__db__.isClosed()) __db__.open();
        return __db__;
    }

    public boolean isClosed() {
        try {
            return (mConn == null) || mConn.isClosed();
        } catch (SQLException e) {
            return true;
        }
    }

    public void open() {
        String driver = "com.mysql.jdbc.Driver";

        try {
            // look for the mysql driver
            Class.forName(driver).newInstance();
            
            // open db connection
            mConn = DriverManager.getConnection(__url__ + __dbname__, __username__, __password__);
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
        }
    }
    
    public Session getSession(Long id) {
        Session ret = new Session();
        ret.id = id;
        ret.userid = 0L;
        ret.name = "Job";
        ret.username = getUsername(ret.userid);
        ret.state = Session.State.IDLE;
        ret.created = new Date();
        return ret;
    }
    
    public String getUsername(Long userid) {
        return "John Doe";
    }
}
