package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventExtra;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SessionWatchView extends View {

    private static SessionWatchViewUiBinder uiBinder = GWT.create(SessionWatchViewUiBinder.class);
    
    @UiField Button buttonBack;
    @UiField CellTable<Node> tableNodes;
    
    @UiField TextBox textStatsState;
    @UiField ProgressBar progressStats;
    @UiField TextBox textStatsElapsedTime;
    @UiField TextBox textStatsDesc;
    
    interface Style extends CssResource {
        String activated();
        String enabled();
        String disabled();
        String error();
    }
    
    @UiField Style style;
    
    // data provider for the node table
    ListDataProvider<Node> mDataProvider = new ListDataProvider<Node>();
    
    Session mSession = null;
    
    ArrayList<Slave> mSlaves = new ArrayList<Slave>();

    interface SessionWatchViewUiBinder extends UiBinder<Widget, SessionWatchView> {
    }

    public SessionWatchView(HydraApp app, Session s) {
        super(app);
        initWidget(uiBinder.createAndBindUi(this));
        
        // create node table
        createNodeTable();

        // initialize the session
        refreshSlaves();
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
    
    private Slave getSlave(Long id) {
        for (Slave s : mSlaves) {
            if (id.equals(s.id)) {
                return s;
            }
        }
        return null;
    }
    
    private void refreshSlaves() {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getSlaves(new AsyncCallback<ArrayList<Slave>>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(ArrayList<Slave> result) {
                mSlaves = result;
                
                // refresh node table
                tableNodes.redraw();
            }

        });
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
        mcs.getNodes(s.id, new AsyncCallback<java.util.ArrayList<de.tubs.cs.ibr.hydra.webmanager.shared.Node>>() {

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
        addNodeHeaders(tableNodes);
    }
    
    public void addNodeHeaders(CellTable<Node> table) {
        /**
         * id column
         */
        TextColumn<Node> idColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                return s.id.toString();
            }
        };
        
        table.addColumn(idColumn, "ID");
        table.setColumnWidth(idColumn, 6, Unit.EM);
        
        /**
         * slave column
         */
        TextColumn<Node> slaveColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node n) {
                Long slaveId = n.slaveId;
                
                // show assigned slave, if assigned
                if (n.assignedSlaveId != null)
                    slaveId = n.assignedSlaveId;
                
                if (slaveId == null) {
                    return "<not assigned>";
                }
                Slave sobj = getSlave(slaveId);
                if (sobj == null) {
                    return "<missing>";
                }
                return sobj.name;
            }

            @Override
            public String getCellStyleNames(Context context, Node n) {
                Long slaveId = n.slaveId;
                
                // show assigned slave, if assigned
                if (n.assignedSlaveId != null)
                    slaveId = n.assignedSlaveId;
                
                if (slaveId == null) {
                    return style.disabled();
                }
                Slave sobj = getSlave(slaveId);
                if (sobj == null) {
                    return style.error();
                }
                
                if (Slave.State.DISCONNECTED.equals(sobj.state)) {
                    if (n.assignedSlaveId != null) return style.error();
                    return style.disabled();
                }
                
                if (n.assignedSlaveId != null) return style.activated();
                return style.enabled();
            }
        };
        
        table.addColumn(slaveColumn, "Slave");
        table.setColumnWidth(slaveColumn, 12, Unit.EM);
        
        /**
         * name column
         */
        TextColumn<Node> nameColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                if (s.name == null) return "<unnamed>";
                return s.name;
            }
        };

        table.addColumn(nameColumn, "Name");
        
        /**
         * address column
         */
        TextColumn<Node> addressColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                if (s.address == null) return "<not assigned>";
                return s.address;
            }
            
            @Override
            public String getCellStyleNames(Context context, Node n) {
                if (n.address == null) return style.disabled();
                return style.activated();
            }
        };

        table.addColumn(addressColumn, "Address");
        table.setColumnWidth(addressColumn, 12, Unit.EM);
        
        /**
         * state column
         */
        TextColumn<Node> stateColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                if (s.state == null) return "<unknown>";
                return s.state.toString();
            }
        };
        
        stateColumn.setSortable(true);
        stateColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        table.addColumn(stateColumn, "State");
        table.setColumnWidth(stateColumn, 8, Unit.EM);
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
        else if (EventType.SLAVE_STATE_CHANGED.equals(evt)) {
            refreshSlaves();
        }
    }
    
    private boolean isRelated(Event evt) {
        if (evt.getExtras() == null) return true;
        
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
