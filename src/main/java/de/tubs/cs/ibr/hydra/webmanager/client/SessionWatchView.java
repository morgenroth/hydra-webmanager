package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
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
    
    @UiField TextBox textStatsState;
    @UiField ProgressBar progressStats;
    @UiField TextBox textStatsElapsedTime;
    @UiField TextBox textStatsDesc;
    
    // data provider for the node table
    ListDataProvider<Node> mDataProvider = new ListDataProvider<Node>();
    
    Session mSession = null;

    interface SessionWatchViewUiBinder extends UiBinder<Widget, SessionWatchView> {
    }

    public SessionWatchView(HydraApp app, Session s) {
        super(app);
        initWidget(uiBinder.createAndBindUi(this));
        
        // create node table
        createNodeTable();

        // initialize the session
        refreshSession(s);
    }
    
    private void refresh() {
        if (mSession == null) return;
        
        // load session properties
        refreshSession(mSession);
    }
    
    private void update(Session s) {
        // store data locally
        mSession = s;

        // update session state
        textStatsState.setText(s.state.toString());
        textStatsDesc.setText(s.name);
        
        // set progress bar to inifite if state is pending
        if (Session.State.PENDING.equals(s.state)) {
            progressStats.setPercent(100);
        } else {
            progressStats.setPercent(0);
        }
        
        // load nodes
        refreshNodeTable(s);
    }
    
    private void refreshSession(Session session) {
        // check for null session objects
        if (session == null) return;
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getSession(session.id, new AsyncCallback<Session>() {
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Can not get session properties.");
                resetView();
            }

            @Override
            public void onSuccess(Session result) {
                update(result);
            }
        });
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
            
            if (isRelated(evt)) {
                // refresh nodes
                refreshNodeTable(mSession);
            }
        }
        else if (EventType.SESSION_REMOVED.equals(evt)) {
            if (isRelated(evt)) {
                // close current view
                resetView();
            }
        }
        else if (EventType.SESSION_STATE_CHANGED.equals(evt)) {
            if (isRelated(evt)) {
                refresh();
            }
        }
        else if (EventType.SESSION_DATA_UPDATED.equals(evt)) {
            if (isRelated(evt)) {
                refresh();
            }
        }
    }
    
    private boolean isRelated(Event evt) {
        for (EventExtra e : evt.getExtras()) {
            if (EventType.EXTRA_SESSION_ID.equals(e.getKey())) {
                if (mSession.id.toString().equals(e.getData())) {
                    return true;
                }
            }
        }
        return false;
    }

    @UiHandler("buttonBack")
    void onClick(ClickEvent e) {
        // switch back to session view
        resetView();
    }
}
