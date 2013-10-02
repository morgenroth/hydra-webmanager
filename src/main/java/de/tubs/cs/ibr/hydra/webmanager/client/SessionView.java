package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonCell;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionView extends View {

    private static SessionViewUiBinder uiBinder = GWT.create(SessionViewUiBinder.class);
    
    // data provider for the session table
    ListDataProvider<Session> mDataProvider = new ListDataProvider<Session>();
    
    @UiField CellTable<Session> sessionTable;
    @UiField Button buttonAdd;

    interface SessionViewUiBinder extends UiBinder<Widget, SessionView> {
    }

    public SessionView(HydraApp app) {
        super(app);
        initWidget(uiBinder.createAndBindUi(this));
        
        // create tables + columns
        createSessionTable();
        
        mDataProvider = new ListDataProvider<Session>();
        mDataProvider.addDataDisplay(sessionTable);
        
        refreshSessionTable();
    }
    
    private void refreshSessionTable() {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getSessions(new AsyncCallback<java.util.ArrayList<de.tubs.cs.ibr.hydra.webmanager.shared.Session>>() {

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
    
    private void createSessionTable() {
        // set table name
        sessionTable.setTitle("Sessions");
        
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
        
        TextColumn<Session> nameColumn = new TextColumn<Session>() {
            @Override
            public String getValue(Session object) {
                return object.name;
            }
        };

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
        
        sessionTable.addColumn(stateColumn, "State");
        sessionTable.setColumnWidth(stateColumn, 8, Unit.EM);

        ButtonCell actionCell = new ButtonCell();
        actionCell.setSize(ButtonSize.SMALL);
        Column<Session, String> actionColumn = new Column<Session, String>(actionCell) {
            @Override
            public String getValue(Session s) {
                ButtonCell button = (ButtonCell)this.getCell();
                
                switch (s.state) {
                    case ABORTED:
                        button.setType(ButtonType.WARNING);
                        button.setIcon(IconType.BACKWARD);
                        return "Reset";
                    case DRAFT:
                        button.setType(ButtonType.SUCCESS);
                        button.setIcon(IconType.PLAY);
                        return "Queue";
                    case FINISHED:
                        button.setType(ButtonType.WARNING);
                        button.setIcon(IconType.BACKWARD);
                        return "Reset";
                    case PENDING:
                        button.setType(ButtonType.WARNING);
                        button.setIcon(IconType.BACKWARD);
                        return "Reset";
                    case RUNNING:
                        button.setType(ButtonType.DANGER);
                        button.setIcon(IconType.STOP);
                        return "Abort";
                    default:
                        button.setType(ButtonType.WARNING);
                        button.setIcon(IconType.BACKWARD);
                        return "Reset";
                }
            }
        };
        
        actionColumn.setFieldUpdater(new FieldUpdater<Session, String>() {
            @Override
            public void update(int index, final Session s, String value) {
                // trigger action on session
                final Session.Action action = Session.Action.fromString(value);
                MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
                mcs.triggerAction(s, action, new AsyncCallback<Void>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        Window.alert("Action '" + action.toString() + "' on session '" + s.name + "' failed!");
                    }

                    @Override
                    public void onSuccess(Void result) {
                        switch (action) {
                            case ABORT:
                                //Window.alert("Session '" + s.name + "' aborted.");
                                break;
                            case QUEUE:
                                //Window.alert("Session '" + s.name + "' queued.");
                                break;
                            case RESET:
                                //Window.alert("Session '" + s.name + "' resetted.");
                                break;
                            default:
                                break;
                        }
                    }
                    
                });
            }
        });

        sessionTable.addColumn(actionColumn);
        sessionTable.setColumnWidth(actionColumn, 8, Unit.EM);
        
        ButtonCell editCell = new ButtonCell();
        editCell.setType(ButtonType.LINK);
        Column<Session, String> editColumn = new Column<Session, String>(editCell) {
            @Override
            public String getValue(Session s) {
                ButtonCell button = (ButtonCell)this.getCell();
                
                switch (s.state) {
                    case PENDING:
                        button.setIcon(IconType.EYE_OPEN);
                        return "Watch";
                    case RUNNING:
                        button.setIcon(IconType.EYE_OPEN);
                        return "Watch";
                    default:
                        button.setIcon(IconType.EDIT);
                        return "Edit";
                }
            }
        };

        editColumn.setFieldUpdater(new FieldUpdater<Session, String>() {
            @Override
            public void update(int index, Session s, String value) {
                // watch running sessions
                if (Session.State.PENDING.equals(s.state) || Session.State.RUNNING.equals(s.state))
                {
                    // open watch view
                    changeView(new SessionWatchView(getApplication(), s));
                }
                else
                {
                    // open edit view
                    changeView(new SessionEditView(getApplication(), s));
                }
            }
        });

        sessionTable.addColumn(editColumn);
        sessionTable.setColumnWidth(editColumn, 8, Unit.EM);
    }

    @Override
    public void eventRaised(Event evt) {
        // refresh table on refresh event
        if (EventType.SESSION_STATE_CHANGED.equals(evt)) {
            refreshSessionTable();
        }
    }
    
    @UiHandler("buttonAdd")
    void onClick(ClickEvent e) {
        // edit session edit view without a existing session
        changeView(new SessionEditView(getApplication(), null));
    }
}
