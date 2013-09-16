package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionView extends Composite {

    private static SessionViewUiBinder uiBinder = GWT.create(SessionViewUiBinder.class);
    
    // data provider for the session table
    ListDataProvider<Session> mDataProvider = new ListDataProvider<Session>();
    
    @UiField CellTable<Session> sessionTable;
    @UiField Button buttonRefresh;

    interface SessionViewUiBinder extends UiBinder<Widget, SessionView> {
    }

    public SessionView() {
        initWidget(uiBinder.createAndBindUi(this));
        
        // create session table + columns
        createTable();
        
        mDataProvider = new ListDataProvider<Session>();
        mDataProvider.addDataDisplay(sessionTable);
        
        refreshTable();
    }
    
    private void refreshTable() {
        DatabaseServiceAsync dsa = (DatabaseServiceAsync)GWT.create(DatabaseService.class);
        dsa.getSessions(new AsyncCallback<java.util.ArrayList<de.tubs.cs.ibr.hydra.webmanager.shared.Session>>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(ArrayList<Session> result) {
                List<Session> list = mDataProvider.getList();
                list.clear();
                for (Session s : result) {
                    list.add(s);
                }
            }
            
        });
    }
    
    private void createTable() {
        // set table name
        sessionTable.setTitle("Sessions");
        
        /*
        final MultiSelectionModel<Session> selectionModel = new MultiSelectionModel<Session>();
        sessionTable.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);
        sessionTable.setSelectionModel(selectionModel);
        
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                // TODO Auto-generated method stub
            }
        });
        
        Column<Session, Boolean> checkColumn = new Column<Session, Boolean>(new CheckboxCell(true, false)) {
            @Override
            public Boolean getValue(Session object) {
                // Get the value from the selection model.
                return selectionModel.isSelected(object);
            }
        };
        sessionTable.addColumn(checkColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
        sessionTable.setColumnWidth(checkColumn, 40, Unit.PX);
        */
        
        TextColumn<Session> idColumn = new TextColumn<Session>() {
            @Override
            public String getValue(Session s) {
                return s.id.toString();
            }
        };
        
        sessionTable.addColumn(idColumn, "Session Key");
        sessionTable.setColumnWidth(idColumn, 12, Unit.EM);
        
        TextColumn<Session> userColumn = new TextColumn<Session>() {
            @Override
            public String getValue(Session s) {
                return s.username;
            }
        };
        
        sessionTable.addColumn(userColumn, "User");
        sessionTable.setColumnWidth(userColumn, 12, Unit.EM);
        
        Column<Session, String> nameColumn = new Column<Session, String>(new ClickableTextCell()) {
            @Override
            public String getValue(Session object) {
                return object.name;
            }
        };
        nameColumn.setFieldUpdater(new FieldUpdater<Session, String>() {
            @Override
            public void update(int index, Session object, String value) {
                
            }
        });

        sessionTable.addColumn(nameColumn, "Description");
        
        Column<Session, Date> dateColumn = new Column<Session, Date>(new DateCell()) {
          @Override
          public Date getValue(Session s) {
              switch (s.state) {
                  case DRAFT:
                      return s.created;
                  case FINISHED:
                      return s.finished;
                  case PENDING:
                      return s.created;
                  case RUNNING:
                      return s.started;
                  case ABORTED:
                      return s.aborted;
                  default:
                      return s.created;
              }
          }
        };
        sessionTable.addColumn(dateColumn, "Last Update");
        sessionTable.setColumnWidth(dateColumn, 16, Unit.EM);
        
        TextColumn<Session> stateColumn = new TextColumn<Session>() {
            @Override
            public String getValue(Session s) {
                if (s.state == null) return "unknown";
                return s.state.toString();
            }
        };
        
        stateColumn.setSortable(true);
        stateColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        sessionTable.addColumn(stateColumn, "State");
        sessionTable.setColumnWidth(stateColumn, 8, Unit.EM);
    }

    @UiHandler("buttonRefresh")
    void onClick(ClickEvent e) {
        refreshTable();
    }
}
