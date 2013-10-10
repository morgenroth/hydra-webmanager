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
import com.google.gwt.cell.client.FieldUpdater;
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
    
    Session mSession = null;

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
                
                // count selected nodes
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
                        listSlave.setSelectedValue(sn.slaveId.toString());
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
    
    private Slave getSlave(Long id) {
        for (Slave s : mSlaves) {
            if (id.equals(s.id)) {
                return s;
            }
        }
        return null;
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
         * name column
         */
        Column<Node, String> nameColumn = new Column<Node, String>(new EditTextCell()) {
            @Override
            public String getValue(Node s) {
                if (s.name == null) return "<unnamed>";
                return s.name;
            }
        };
        nameColumn.setFieldUpdater(new FieldUpdater<Node, String>() {
            @Override
            public void update(int index, Node object, String value) {
                object.name = value;
                applyNode(object);
            }
        });

        table.addColumn(nameColumn, "Name");
        
        /**
         * slave column
         */
        TextColumn<Node> slaveColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                Long slaveId = s.slaveId;
                
                // show assigned slave, if assigned
                if (s.assignedSlaveId != null)
                    slaveId = s.assignedSlaveId;
                
                if (slaveId == null) {
                    return "<not assigned>";
                }
                Slave sobj = getSlave(slaveId);
                if (sobj == null) {
                    return "<missing>";
                }
                return sobj.name + " (" + sobj.state.toString() + ")";
            }
        };
        
        table.addColumn(slaveColumn, "Slave");
        table.setColumnWidth(slaveColumn, 20, Unit.EM);
    }
    
    private void applyNode(Node object) {
        ArrayList<Node> list = new ArrayList<Node>();
        list.add(object);
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.applyNodes(list, new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Void result) {
            }

        });
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
            listSlave.addItem(s.name + " (" + s.state.toString() + ")", s.id.toString());
        }
        
        // restore selected value
        listSlave.setSelectedValue(selected);
    }
    
    public void refresh(Session s) {
        if (s == null) return;
        
        // store session locally
        mSession = s;
        
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
    
    private ArrayList<Node> getSelectedNodes() {
        ArrayList<Node> ret = new ArrayList<Node>();
        
        for (Node n : mDataProvider.getList()) {
            if (selectionModel.isSelected(n)) {
                ret.add(n);
            }
        }
        
        return ret;
    }
    
    @UiHandler("buttonAdd")
    void onAddNodes(ClickEvent e) {
        Long amount = Long.valueOf(textAddNumber.getText());
        
        if ((amount == null) || (amount < 1)) {
            alertColumn.clear();
            mAlert.setType(AlertType.ERROR);
            mAlert.setText("Failure! Could not process the requested amount of nodes.");
            mAlert.setClose(true);
            mAlert.setAnimation(true);
            alertColumn.add(mAlert);
            return;
        }
        
        String slaveValue = listSlave.getValue(listSlave.getSelectedIndex());
        
        Long slaveId = null;
        
        if (!"-".equals(slaveValue))
        {
            slaveId = Long.valueOf(slaveValue);
            if (slaveId == null) {
                alertColumn.clear();
                mAlert.setType(AlertType.ERROR);
                mAlert.setText("Failure! The selected slave is invalid.");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
                return;
            }
        }
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.createNodes(amount, mSession.id, slaveId, new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable caught) {
                alertColumn.clear();
                mAlert.setType(AlertType.ERROR);
                mAlert.setText("Failure! Could not create nodes.");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
            }

            @Override
            public void onSuccess(Void result) {
                alertColumn.clear();
                mAlert.setType(AlertType.SUCCESS);
                mAlert.setText("Successful created!");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
                
                scheduleAlertClear(mAlert, 5000);
            }

        });
    }
    
    @UiHandler("listSlave")
    void onSlaveChange(ChangeEvent e) {
        // do not commit if no node is selected
        if (mSelectedCount == 0) return;
        
        String slaveValue = listSlave.getValue(listSlave.getSelectedIndex());
        
        Long slaveId = null;
        
        if (!"-".equals(slaveValue))
        {
            slaveId = Long.valueOf(slaveValue);
            if (slaveId == null) {
                alertColumn.clear();
                mAlert.setType(AlertType.ERROR);
                mAlert.setText("Failure! The selected slave is invalid.");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
                return;
            }
        }
        
        // get all selected nodes
        ArrayList<Node> nodes = getSelectedNodes();
        
        // apply slave change to all nodes
        for (Node n : nodes) {
            n.slaveId = slaveId;
        }
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.applyNodes(nodes, new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable caught) {
                alertColumn.clear();
                mAlert.setType(AlertType.ERROR);
                mAlert.setText("Failure! Could not change slave.");
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
                
                String value = listSlave.getValue(listSlave.getSelectedIndex());
                if ("-".equals(value)) {
                    listSlave.setSelectedIndex(0);
                }
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
        // get all selected nodes
        ArrayList<Node> nodes = getSelectedNodes();
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.removeNodes(nodes, new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable caught) {
                alertColumn.clear();
                mAlert.setType(AlertType.ERROR);
                mAlert.setText("Failure! Could not remove selected slaves.");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
            }

            @Override
            public void onSuccess(Void result) {
                alertColumn.clear();
                mAlert.setType(AlertType.SUCCESS);
                mAlert.setText("Successful removed!");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
                
                scheduleAlertClear(mAlert, 5000);
            }
            
        });
    }
}
