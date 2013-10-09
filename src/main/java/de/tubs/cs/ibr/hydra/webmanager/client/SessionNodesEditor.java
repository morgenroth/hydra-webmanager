package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Alert;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;

import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SessionNodesEditor extends Composite {

    private static SessionNodesEditorUiBinder uiBinder = GWT
            .create(SessionNodesEditorUiBinder.class);

    interface SessionNodesEditorUiBinder extends UiBinder<Widget, SessionNodesEditor> {
    }
    
    @UiField com.github.gwtbootstrap.client.ui.Column alertColumn;
    @UiField CellTable<Node> tableNodes;
    
    @UiField Button buttonAdd;
    @UiField TextBox textAddNumber;
    
    @UiField ListBox listSlave;
    
    @UiField Button buttonSelectAll;
    @UiField Button buttonSelectNone;
    @UiField Button buttonRemoveSelected;
    
    // data provider for the node table
    ListDataProvider<Node> mDataProvider = new ListDataProvider<Node>();
    
    final Alert mAlert = new Alert();
    
    // selection model for the table
    final SelectionModel<Node> selectionModel = new MultiSelectionModel<Node>(new ProvidesKey<Node>() {
        @Override
        public Object getKey(Node item) {
            return item.id;
        }
    });
    
    int mSelectedCount = 0;
    ArrayList<Slave> mSlaves = new ArrayList<Slave>();

    public SessionNodesEditor() {
        initWidget(uiBinder.createAndBindUi(this));
        
        // create node table
        createTable();
    }
    
    private void createTable() {
        mDataProvider = new ListDataProvider<Node>();
        mDataProvider.addDataDisplay(tableNodes);
        
        // set table name
        tableNodes.setTitle("Nodes");
        
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                int count = 0;
                Node sn = null;
                
                for (Node n : mDataProvider.getList()) {
                    if (selectionModel.isSelected(n)) {
                        sn = n;
                        count++;
                    }
                }
                
                mSelectedCount = count;
                
                if (count > 0) {
                    refreshSlaveList(mSlaves, true);
                    
                    // at least one item is selected
                    buttonSelectNone.setEnabled(true);
                    buttonRemoveSelected.setEnabled(true);
                    
                    if ((count == 1) && (sn.slaveId != null)) {
                        listSlave.setSelectedValue(sn.slaveName);
                    } else {
                        listSlave.setSelectedIndex(0);
                    }
                } else {
                    refreshSlaveList(mSlaves, false);
                    
                    // nothing is selected
                    buttonSelectNone.setEnabled(false);
                    buttonRemoveSelected.setEnabled(false);
                    listSlave.setSelectedIndex(0);
                }
            }
        });
        
        // set selection model
        tableNodes.setSelectionModel(selectionModel, DefaultSelectionEventManager.<Node> createCheckboxManager());
        
        // add common headers
        addHeaders(tableNodes);
    }
    
    private void addHeaders(CellTable<Node> table) {
        
        /**
         * check column
         */
        Column<Node, Boolean> checkColumn = new Column<Node, Boolean>(new CheckboxCell(true, false)) {
            @Override
            public Boolean getValue(Node object) {
                // Get the value from the selection model.
                return selectionModel.isSelected(object);
            }
        };
        table.addColumn(checkColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
        table.setColumnWidth(checkColumn, 40, Unit.PX);
            
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
        table.setColumnWidth(idColumn, 4, Unit.EM);
        
        /**
         * slave column
         */
        TextColumn<Node> slaveColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                if (s.slaveName == null) {
                    return "<not assigned>";
                }
                return s.slaveName;
            }
        };
        
        table.addColumn(slaveColumn, "Slave");
        table.setColumnWidth(slaveColumn, 16, Unit.EM);
        
        /**
         * name column
         */
        Column<Node, String> nameColumn = new Column<Node, String>(new EditTextCell()) {
            @Override
            public String getValue(Node s) {
                if (s.name == null) return "<unnamed>";
                return s.name;
            }
        };

        table.addColumn(nameColumn, "Name");
    }
    
    public void refreshSlaveList(ArrayList<Slave> slaves, boolean showClearElement) {
        // store selected value
        String selected = listSlave.getValue();
        
        // remove all non-default items
        while (listSlave.getItemCount() > 1) {
            listSlave.removeItem(1);
        }
        
        // add clear element
        if (showClearElement)
            listSlave.addItem("- clear assignment -", "-");
        
        // add all slaves
        for (Slave s : slaves) {
            listSlave.addItem(s.name);
        }
        
        // restore selected value
        listSlave.setSelectedValue(selected);
    }
    
    public void refresh(Session s) {
        if (s == null) return;
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getSlaves(new AsyncCallback<ArrayList<Slave>>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(ArrayList<Slave> result) {
                mSlaves = result;
                refreshSlaveList(result, false);
            }

        });
        
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
                
                if (list.size() > 0) {
                    buttonSelectAll.setEnabled(true);
                } else {
                    buttonSelectAll.setEnabled(false);
                    buttonSelectNone.setEnabled(false);
                    buttonRemoveSelected.setEnabled(false);
                }
            }
            
        });
    }
    
    private void scheduleAlertClear(final Alert alert, int delay) {
        Timer t = new Timer() {
            @Override
            public void run() {
                alert.close();
            }
        };
        
        t.schedule(delay);
    }
    
    @UiHandler("buttonAdd")
    void onAddNodes(ClickEvent e) {
        
    }
    
    @UiHandler("listSlave")
    void onSlaveChange(ChangeEvent e) {
        // do not commit if no node is selected
        if (mSelectedCount == 0) return;
        
        ArrayList<Node> nodes = null;
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.applyNodes(nodes, new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable caught) {
                alertColumn.clear();
                mAlert.setType(AlertType.ERROR);
                mAlert.setText("Failure! Can not change slave.");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
                
                listSlave.setSelectedIndex(0);
            }

            @Override
            public void onSuccess(Void result) {
                alertColumn.clear();
                mAlert.setType(AlertType.SUCCESS);
                mAlert.setText("Successful changed!");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
                
                scheduleAlertClear(mAlert, 5000);
                
                listSlave.setSelectedIndex(0);
            }
            
        });
    }
    
    @UiHandler("buttonSelectAll")
    void onSelectAll(ClickEvent e) {
        for (Node n : mDataProvider.getList()) {
            selectionModel.setSelected(n, true);
        }
    }
    
    @UiHandler("buttonSelectNone")
    void onSelectNone(ClickEvent e) {
        for (Node n : mDataProvider.getList()) {
            selectionModel.setSelected(n, false);
        }
    }
    
    @UiHandler("buttonRemoveSelected")
    void onRemoveSelection(ClickEvent e) {
        
    }
}
