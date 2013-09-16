package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

@RemoteServiceRelativePath("database")
public interface DatabaseService extends RemoteService {
    public Session getSession(Long id);
}
