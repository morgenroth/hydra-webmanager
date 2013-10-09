package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.CellTable;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionNodesEditor extends Composite {

    private static SessionNodesEditorUiBinder uiBinder = GWT
            .create(SessionNodesEditorUiBinder.class);

    interface SessionNodesEditorUiBinder extends UiBinder<Widget, SessionNodesEditor> {
    }
    
    @UiField CellTable<Node> tableNodes;
    
    // data provider for the node table
    ListDataProvider<Node> mDataProvider = new ListDataProvider<Node>();

    public SessionNodesEditor() {
        initWidget(uiBinder.createAndBindUi(this));
        
        // create node table
        createNodeTable();
    }

    private void createNodeTable() {
        mDataProvider = new ListDataProvider<Node>();
        mDataProvider.addDataDisplay(tableNodes);
        
        // set table name
        tableNodes.setTitle("Nodes");
        
        // add common headers
        TableUtils.addNodeHeaders(tableNodes);
    }
    
    public void refresh(Session s) {
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
}
