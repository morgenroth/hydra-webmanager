package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventExtra;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionWatchView extends View {

    private static SessionWatchViewUiBinder uiBinder = GWT.create(SessionWatchViewUiBinder.class);
    
    @UiField Button buttonBack;
    @UiField CellTable<Node> tableNodes;
    
    // data provider for the node table
    ListDataProvider<Node> mDataProvider = new ListDataProvider<Node>();
    
    Session mSession = null;

    interface SessionWatchViewUiBinder extends UiBinder<Widget, SessionWatchView> {
    }

    public SessionWatchView(HydraApp app, Session s) {
        super(app);
        mSession = s;
        
        initWidget(uiBinder.createAndBindUi(this));
        
        // create node table
        createNodeTable();
        
        // load nodes
        refreshNodeTable(s);
    }
    
    private void refreshNodeTable(Session s) {
        if (s == null) return;
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getNodes(s.id.toString(), new AsyncCallback<java.util.ArrayList<de.tubs.cs.ibr.hydra.webmanager.shared.Node>>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(ArrayList<Node> result) {
                List<Node> list = mDataProvider.getList();
                list.clear();
                for (Node n : result) {
                    list.add(n);
                }
            }
            
        });
    }
    
    private void createNodeTable() {
        mDataProvider = new ListDataProvider<Node>();
        mDataProvider.addDataDisplay(tableNodes);
        
        // set table name
        tableNodes.setTitle("Nodes");
        
        // add common headers
        TableUtils.addNodeHeaders(tableNodes);
    }

    @Override
    public void eventRaised(Event evt) {
        // refresh table on refresh event
        if (EventType.NODE_STATE_CHANGED.equals(evt)) {
            // do not update, if we don't have a session
            if (mSession == null) return;
            
            for (EventExtra e : evt.getExtras()) {
                if (EventType.EXTRA_SESSION_ID.equals(e.getKey())) {
                    if (mSession.id.toString().equals(e.getData())) {
                        // refresh nodes
                        refreshNodeTable(mSession);
                        return;
                    }
                }
            }
        }
    }

    @UiHandler("buttonBack")
    void onClick(ClickEvent e) {
        // switch back to session view
        resetView();
    }
}
