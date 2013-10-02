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
import de.tubs.cs.ibr.hydra.webmanager.shared.Event.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class NodeView extends View {

    private static NodeViewUiBinder uiBinder = GWT.create(NodeViewUiBinder.class);

    interface NodeViewUiBinder extends UiBinder<Widget, NodeView> {
    }
    
    // data provider for the node table
    ListDataProvider<Node> mDataProvider = new ListDataProvider<Node>();
    
    Session mSession = null;
    
    @UiField CellTable<Node> nodeTable;
    @UiField Button buttonBack;

    public NodeView(HydraApp app, Session s) {
        super(app);
        mSession = s;
        
        initWidget(uiBinder.createAndBindUi(this));
        
        // create session table + columns
        createTable();
        
        refreshNodeTable(s);
    }

    private void refreshNodeTable(Session s) {
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
    
    private void createTable() {
        mDataProvider = new ListDataProvider<Node>();
        mDataProvider.addDataDisplay(nodeTable);
        
        // set table name
        nodeTable.setTitle("Nodes");
        
        // add common headers
        TableUtils.addNodeHeaders(nodeTable);
    }

    @Override
    public void eventRaised(Event evt) {
        // refresh table on refresh event
        if (EventType.NODE_STATE_CHANGED.equals(evt)) {
            refreshNodeTable(mSession);
        }
    }
    
    @UiHandler("buttonBack")
    void onClick(ClickEvent e) {
        // switch back to session view
        resetView();
    }
}
