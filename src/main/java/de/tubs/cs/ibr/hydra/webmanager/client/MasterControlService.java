package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

@RemoteServiceRelativePath("master")
public interface MasterControlService extends RemoteService {
    public void triggerAction(Session s, Session.Action action);
}
