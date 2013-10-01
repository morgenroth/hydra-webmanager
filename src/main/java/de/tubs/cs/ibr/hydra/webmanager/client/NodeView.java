package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.CellTable;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public class NodeView extends Composite implements EventListener {

    private static NodeViewUiBinder uiBinder = GWT.create(NodeViewUiBinder.class);

    interface NodeViewUiBinder extends UiBinder<Widget, NodeView> {
    }
    
    // data provider for the node table
    ListDataProvider<Node> mDataProvider = new ListDataProvider<Node>();
    
    String mSessionKey = "1234";
    
    @UiField CellTable<Node> nodeTable;

    public NodeView(String sessionKey) {
        initWidget(uiBinder.createAndBindUi(this));
        
        mSessionKey = sessionKey;
        
        // create session table + columns
        createTable();
        
        mDataProvider = new ListDataProvider<Node>();
        mDataProvider.addDataDisplay(nodeTable);
        
        refreshNodeTable(mSessionKey);
    }

    private void refreshNodeTable(String sessionKey) {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getNodes(sessionKey, new AsyncCallback<java.util.ArrayList<de.tubs.cs.ibr.hydra.webmanager.shared.Node>>() {

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
    
    private void createTable() {
        // set table name
        nodeTable.setTitle("Nodes");
        
        TextColumn<Node> idColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                return s.id.toString();
            }
        };
        
        nodeTable.addColumn(idColumn, "ID");
        nodeTable.setColumnWidth(idColumn, 6, Unit.EM);
        
        TextColumn<Node> slaveColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                if (s.slaveName == null) {
                    return "not assigned";
                }
                return s.slaveName;
            }
        };
        
        nodeTable.addColumn(slaveColumn, "Slave");
        nodeTable.setColumnWidth(slaveColumn, 12, Unit.EM);
        
        Column<Node, String> nameColumn = new Column<Node, String>(new ClickableTextCell()) {
            @Override
            public String getValue(Node object) {
                return object.name;
            }
        };
        nameColumn.setFieldUpdater(new FieldUpdater<Node, String>() {
            @Override
            public void update(int index, Node object, String value) {
                
            }
        });

        nodeTable.addColumn(nameColumn, "Name");
               
        TextColumn<Node> stateColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                if (s.state == null) return "unknown";
                return s.state.toString();
            }
        };
        
        stateColumn.setSortable(true);
        stateColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        nodeTable.addColumn(stateColumn, "State");
        nodeTable.setColumnWidth(stateColumn, 8, Unit.EM);
    }

    @Override
    public void eventRaised(Event evt) {
        // refresh table on refresh event
        if (EventType.NODE_STATE_CHANGED.equals(evt)) {
            refreshNodeTable(mSessionKey);
        }
    }
}
