package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.TextColumn;
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

public class NodeView extends View {

    private static NodeViewUiBinder uiBinder = GWT.create(NodeViewUiBinder.class);

    interface NodeViewUiBinder extends UiBinder<Widget, NodeView> {
    }
    
    // data provider for the node table
    ListDataProvider<Node> mDataProvider = new ListDataProvider<Node>();
    
    Session mSession = null;
    
    ArrayList<Slave> mSlaves = new ArrayList<Slave>();
    
    @UiField CellTable<Node> nodeTable;
    @UiField Button buttonBack;

    public NodeView(HydraApp app, Session s) {
        super(app);
        mSession = s;
        
        initWidget(uiBinder.createAndBindUi(this));
        
        // create session table + columns
        createTable();
     
        refreshSlaves();
        refreshNodeTable(s);
    }

    private void refreshNodeTable(Session s) {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        Long sessionId = null;
        
        if (s != null) {
            sessionId = s.id;
        }
        
        mcs.getNodes(sessionId, new AsyncCallback<ArrayList<Node>>() {

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
                nodeTable.redraw();
            }

        });
    }
    
    private void createTable() {
        mDataProvider = new ListDataProvider<Node>();
        mDataProvider.addDataDisplay(nodeTable);
        
        // set table name
        nodeTable.setTitle("Nodes");
        
        // add common headers
        addNodeHeaders(nodeTable);
    }
    
    private Slave getSlave(Long id) {
        for (Slave s : mSlaves) {
            if (id.equals(s.id)) {
                return s;
            }
        }
        return null;
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
            public String getValue(Node s) {
                if (s.slaveId == null) {
                    return "<not assigned>";
                }
                Slave sobj = getSlave(s.slaveId);
                if (sobj == null) {
                    return "<missing>";
                }
                return sobj.name + " (" + sobj.state.toString() + ")";
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
            // check if this node is of interest
            if (mSession != null) {
                for (EventExtra e : evt.getExtras()) {
                    if (EventType.EXTRA_SESSION_ID.equals(e.getKey())) {
                        if (!mSession.id.toString().equals(e.getData())) {
                            // abort - session id is set but not of interest
                            return;
                        }
                    }
                }
            }
            refreshNodeTable(mSession);
        }
        else if (EventType.SLAVE_STATE_CHANGED.equals(evt)) {
            refreshSlaves();
        }
    }
    
    @UiHandler("buttonBack")
    void onClick(ClickEvent e) {
        // switch back to session view
        resetView();
    }
}
