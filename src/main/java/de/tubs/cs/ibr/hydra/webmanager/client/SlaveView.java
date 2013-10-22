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
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SlaveView extends View {

    private static SlaveViewUiBinder uiBinder = GWT.create(SlaveViewUiBinder.class);
    
    // data provider for the slave table
    ListDataProvider<Slave> mSlaveProvider = new ListDataProvider<Slave>();

    @UiField CellTable<Slave> slaveTable;
    @UiField Button buttonBack;

    interface SlaveViewUiBinder extends UiBinder<Widget, SlaveView> {
    }

    public SlaveView(HydraApp app) {
        super(app);
        initWidget(uiBinder.createAndBindUi(this));
        
        // create tables + columns
        createSlaveTable();
        
        mSlaveProvider = new ListDataProvider<Slave>();
        mSlaveProvider.addDataDisplay(slaveTable);
        
        refreshSlaveTable();
    }
    
    private void refreshSlaveTable() {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getSlaves(new AsyncCallback<java.util.ArrayList<de.tubs.cs.ibr.hydra.webmanager.shared.Slave>>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(ArrayList<Slave> result) {
                List<Slave> list = mSlaveProvider.getList();
                list.clear();
                for (Slave s : result) {
                    list.add(s);
                }
            }
            
        });
    }
    
    private void createSlaveTable() {
        // set table name
        slaveTable.setTitle("Slaves");
        
        /*
         * Name column
         */
        TextColumn<Slave> nameColumn = new TextColumn<Slave>() {
            @Override
            public String getValue(Slave s) {
                if (s.name == null) return "unknown";
                return s.name;
            }
        };
        
        slaveTable.addColumn(nameColumn, "Name");
        slaveTable.setColumnWidth(nameColumn, 8, Unit.EM);
        
        /*
         * Address column
         */
        TextColumn<Slave> addressColumn = new TextColumn<Slave>() {
            @Override
            public String getValue(Slave s) {
                if (s.address == null) return "unknown";
                return s.address.toString();
            }
        };
        
        slaveTable.addColumn(addressColumn, "Address");
        slaveTable.setColumnWidth(addressColumn, 8, Unit.EM);
        
        /*
         * State column
         */
        TextColumn<Slave> stateColumn = new TextColumn<Slave>() {
            @Override
            public String getValue(Slave s) {
                if (s.state == null) return "unknown";
                return s.state.toString();
            }
        };
        
        slaveTable.addColumn(stateColumn, "State");
        slaveTable.setColumnWidth(stateColumn, 8, Unit.EM);
    }

    @Override
    public void onEventRaised(Event evt) {
        // refresh table on refresh event
        if (evt.equals(EventType.SLAVE_STATE_CHANGED)) {
            refreshSlaveTable();
        }
    }

    @UiHandler("buttonBack")
    void onClick(ClickEvent e) {
        // switch back to session view
        resetView();
    }
}
