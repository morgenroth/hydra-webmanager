package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.atmosphere.gwt20.client.Atmosphere;
import org.atmosphere.gwt20.client.AtmosphereCloseHandler;
import org.atmosphere.gwt20.client.AtmosphereMessageHandler;
import org.atmosphere.gwt20.client.AtmosphereOpenHandler;
import org.atmosphere.gwt20.client.AtmosphereRequest;
import org.atmosphere.gwt20.client.AtmosphereRequestConfig;
import org.atmosphere.gwt20.client.AtmosphereResponse;
import org.atmosphere.gwt20.client.AutoBeanClientSerializer;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionView extends Composite implements HasText {

    private static SessionViewUiBinder uiBinder = GWT.create(SessionViewUiBinder.class);
    
    private MyBeanFactory beanFactory = GWT.create(MyBeanFactory.class);
    
    private AtmosphereRequest jsonRequest;
    
    @UiField CellTable<Session> sessionTable;

    interface SessionViewUiBinder extends UiBinder<Widget, SessionView> {
    }

    public SessionView(Atmosphere atmosphere) {
        initWidget(uiBinder.createAndBindUi(this));
        
        AutoBeanClientSerializer json_serializer = new AutoBeanClientSerializer();
        json_serializer.registerBeanFactory(beanFactory, Event.class);
                       
        // setup JSON Atmosphere connection
        AtmosphereRequestConfig jsonRequestConfig = AtmosphereRequestConfig.create(json_serializer);
        jsonRequestConfig.setUrl(GWT.getModuleBaseURL() + "atmosphere/json");
        jsonRequestConfig.setContentType("application/json; charset=UTF-8");
        jsonRequestConfig.setTransport(AtmosphereRequestConfig.Transport.STREAMING);
        jsonRequestConfig.setFallbackTransport(AtmosphereRequestConfig.Transport.LONG_POLLING);
        jsonRequestConfig.setOpenHandler(new AtmosphereOpenHandler() {
            @Override
            public void onOpen(AtmosphereResponse response) {
                WebManager.logger.info("JSON Connection opened");
            }
        });
        jsonRequestConfig.setCloseHandler(new AtmosphereCloseHandler() {
            @Override
            public void onClose(AtmosphereResponse response) {
                WebManager.logger.info("JSON Connection closed");
            }
        });
        jsonRequestConfig.setMessageHandler(new AtmosphereMessageHandler() {
            @Override
            public void onMessage(AtmosphereResponse response) {
                List<Event> events = response.getMessages();
                for (Event event : events) {
                    WebManager.logger.info("received message through JSON: " + event.getData());
                    Window.alert(event.getData());
                }
            }
        });
        
        jsonRequest = atmosphere.subscribe(jsonRequestConfig);
        sessionTable.setTitle("Sessions");
        
        // Make the name column sortable.
        //sessionTable.setSortable(true);

        createTableColumns();
        
        
        final SingleSelectionModel<Session> selectionModel = new SingleSelectionModel<Session>();
        sessionTable.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);
        sessionTable.setSelectionModel(selectionModel);
        
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                // TODO Auto-generated method stub
                
            }
        });
        
        final ListDataProvider<Session> dataProvider = new ListDataProvider<Session>();
        dataProvider.addDataDisplay(sessionTable);
        
        DatabaseServiceAsync dsa = (DatabaseServiceAsync)GWT.create(DatabaseService.class);
        dsa.getSession(42L, new AsyncCallback<de.tubs.cs.ibr.hydra.webmanager.shared.Session>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Session result) {
                dataProvider.getList().add(result);
            }
            
        });
    }
    
    private void createTableColumns() {
        TextColumn<Session> idColumn = new TextColumn<Session>() {
            @Override
            public String getValue(Session s) {
                return s.id.toString();
            }
        };
        
        sessionTable.addColumn(idColumn, "ID");
        
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
        nameColumn.setSortable(true);
        nameColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
        sessionTable.addColumn(nameColumn, "Name");
        
        TextColumn<Session> userColumn = new TextColumn<Session>() {
            @Override
            public String getValue(Session s) {
                return s.name;
            }
        };
        
        sessionTable.addColumn(userColumn, "User");
        
        TextColumn<Session> stateColumn = new TextColumn<Session>() {
            @Override
            public String getValue(Session s) {
                if (s.state == null) return "unknown";
                return s.state.toString();
            }
        };
        
        DateCell dateCell = new DateCell();
        Column<Session, Date> dateColumn = new Column<Session, Date>(dateCell) {
          @Override
          public Date getValue(Session object) {
            return object.created;
          }
        };
        sessionTable.addColumn(dateColumn, "Created");
        
        stateColumn.setSortable(true);
        stateColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        sessionTable.addColumn(stateColumn, "State");
    }

    @UiField
    Button button;

    public SessionView(String firstName) {
        initWidget(uiBinder.createAndBindUi(this));
        button.setText(firstName);
    }

    @UiHandler("button")
    void onClick(ClickEvent e) {
        Event myevent = beanFactory.create(Event.class).as();
        myevent.setData("Hello JSON!");
        
        try {
            jsonRequest.push(myevent);
        } catch (SerializationException ex) {
            WebManager.logger.log(Level.SEVERE, "Failed to serialize message", ex);
        }
    }

    public void setText(String text) {
        button.setText(text);
    }

    public String getText() {
        return button.getText();
    }

}
