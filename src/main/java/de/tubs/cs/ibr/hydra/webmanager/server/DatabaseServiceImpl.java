package de.tubs.cs.ibr.hydra.webmanager.server;

import java.util.ArrayList;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import de.tubs.cs.ibr.hydra.webmanager.client.DatabaseService;
import de.tubs.cs.ibr.hydra.webmanager.server.db.Database;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class DatabaseServiceImpl extends RemoteServiceServlet implements DatabaseService {

    /**
     * Serial ID
     */
    private static final long serialVersionUID = 2314930436596405034L;

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

}
