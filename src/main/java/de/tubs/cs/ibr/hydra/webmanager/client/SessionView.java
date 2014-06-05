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
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import de.tubs.cs.ibr.hydra.webmanager.shared.Credentials;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.ToggleButtonCell;

public class SessionView extends View {

    private static SessionViewUiBinder uiBinder = GWT.create(SessionViewUiBinder.class);
    
    private static Credentials mCredentials = null;
    
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
        
        mCredentials = app.getCredentials();
        buttonAdd.setEnabled(isLoggedIn());
        
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
        
        TextColumn<Session> dateColumn = new TextColumn<Session>() {
          @Override
          public String getValue(Session s) {
              switch (s.state) {
                  case DRAFT:
                      return formatDatetime(s.created);
                  case FINISHED:
                      return formatDatetime(s.finished);
                  case PENDING:
                      return formatDatetime(s.created);
                  case RUNNING:
                      return formatDatetime(s.started);
                  case ABORTED:
                      return formatDatetime(s.aborted);
                  case CANCELLED:
                      return formatDatetime(s.aborted);
                  default:
                      return formatDatetime(s.created);
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

        ToggleButtonCell actionCell = new ToggleButtonCell();
        actionCell.setSize(ButtonSize.SMALL);
        Column<Session, String> actionColumn = new Column<Session, String>(actionCell) {
            @Override
            public String getValue(Session s) {
                ToggleButtonCell button = (ToggleButtonCell)this.getCell();
                
                button.setEnabled(isUser(s.username));
                
                switch (s.state) {
                    case ABORTED:
                        button.setType(ButtonType.WARNING);
                        button.setIcon(IconType.BACKWARD);
                        return "Reset";
                    case CANCELLED:
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
                        button.setType(ButtonType.DANGER);
                        button.setIcon(IconType.REMOVE_SIGN);
                        return "Cancel";
                    case RUNNING:
                        button.setType(ButtonType.DANGER);
                        button.setIcon(IconType.STOP);
                        return "Abort";
                    case INITIAL:
                        button.setType(ButtonType.DANGER);
                        button.setIcon(IconType.REMOVE);
                        return "Remove";
                    case ERROR:
                        button.setType(ButtonType.WARNING);
                        button.setIcon(IconType.BACKWARD);
                        return "Reset";
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
                //allow action-triggering only if logged in
                if(isLoggedIn())
                {
                       MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
                    mcs.triggerAction(s, action, new AsyncCallback<Void>() {
    
                        @Override
                        public void onFailure(Throwable caught) {
                            Window.alert("Action '" + action.toString() + "' on session " + s.id.toString() + " failed!");
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
                                case CANCEL:
                                    //Window.alert("Session '" + s.name + "' cancelled.");
                                    break;
                                default:
                                    break;
                            }
                        }
                        
                    });
                        
                }

            }
        });

        sessionTable.addColumn(actionColumn);
        sessionTable.setColumnWidth(actionColumn, 8, Unit.EM);
        
        ToggleButtonCell editCell = new ToggleButtonCell();
        editCell.setType(ButtonType.LINK);
        Column<Session, String> editColumn = new Column<Session, String>(editCell) {
            @Override
            public String getValue(Session s) {
                ToggleButtonCell button = (ToggleButtonCell)this.getCell();
                
                switch (s.state) {
                    case INITIAL:
                        button.setIcon(IconType.EDIT);
                        //disable, if not logged in as user to whom this session belongs
                        button.setEnabled(isUser(s.username));
                        return "Edit";
                    case DRAFT:
                        button.setIcon(IconType.EDIT);
                        //disable, if not logged in as user to whom this session belongs
                        button.setEnabled(isUser(s.username));
                        return "Edit";
                    default:
                        //always enabled
                        button.setEnabled(true);
                        button.setIcon(IconType.EYE_OPEN);
                        return "Watch";
                }
            }
        };

        editColumn.setFieldUpdater(new FieldUpdater<Session, String>() {
            @Override
            public void update(int index, Session s, String value) {
                // watch running sessions
                if (Session.State.INITIAL.equals(s.state) || Session.State.DRAFT.equals(s.state))
                {
                    // open edit view, when logged in
                    if (isLoggedIn())
                        changeView(new SessionEditView(getApplication(), s));
                }
                else
                {
                    // open watch view
                    changeView(new SessionWatchView(getApplication(), s));
                }
            }
        });

        sessionTable.addColumn(editColumn);
        sessionTable.setColumnWidth(editColumn, 8, Unit.EM);
    }

    @Override
    public void onEventRaised(Event evt) {
        // refresh table on refresh event
        if (evt.equals(EventType.SESSION_STATE_CHANGED)) {
            refreshSessionTable();
        }
        else if (evt.equals(EventType.SESSION_REMOVED)) {
            refreshSessionTable();
        }
        else if (evt.equals(EventType.SESSION_ADDED)) {
            refreshSessionTable();
        }
        else if (evt.equals(EventType.SESSION_DATA_UPDATED)) {
            refreshSessionTable();
        }
    }
    
    @UiHandler("buttonAdd")
    void onClick(ClickEvent e) {
        // edit session edit view without a existing session
        changeView(new SessionEditView(getApplication(), null));
    }
    
    public static String formatDatetime(Date d) {
        return DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM).format(d);
    }
    
    private boolean isLoggedIn()
    {
        return mCredentials != null;
    }
    
    private boolean isUser(String username)
    {
        if (mCredentials == null)
            return false;
        return mCredentials.getUsername() == username;
    }
}
