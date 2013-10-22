package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.List;

import org.atmosphere.gwt20.client.Atmosphere;
import org.atmosphere.gwt20.client.AtmosphereCloseHandler;
import org.atmosphere.gwt20.client.AtmosphereMessageHandler;
import org.atmosphere.gwt20.client.AtmosphereOpenHandler;
import org.atmosphere.gwt20.client.AtmosphereRequest;
import org.atmosphere.gwt20.client.AtmosphereRequestConfig;
import org.atmosphere.gwt20.client.AtmosphereResponse;
import org.atmosphere.gwt20.client.ClientSerializer;

import com.github.gwtbootstrap.client.ui.Alert;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;

public class HydraApp extends Composite {

    private static HydraAppUiBinder uiBinder = GWT.create(HydraAppUiBinder.class);

    private Atmosphere atmosphere = null;

    interface HydraAppUiBinder extends UiBinder<Widget, HydraApp> {
    }
    
    @UiField Container containerContent;
    @UiField NavLink navSession;
    @UiField NavLink navSlaves;
    @UiField NavLink navNodes;
    
    @UiField Column alertColumn;
    final Alert mAlert = new Alert();
    
    View currentView = null;
    
    AtmosphereRequest jsonRequest;
    String atmosphereId = null;

    public HydraApp() {
        initWidget(uiBinder.createAndBindUi(this));
    }
    
    @Override
    protected void initWidget(Widget widget) {
        super.initWidget(widget);
        
        mAlert.setType(AlertType.INFO);
        mAlert.setHeading("Server:");
        mAlert.setText("Connecting...");
        mAlert.setAnimation(true);
        alertColumn.add(mAlert);
        
        changeView(null);
    }
    
    @UiHandler("navSession")
    void onSessionsClick(ClickEvent e) {
        changeView(null);
    }
    
    @UiHandler("navSlaves")
    void onSlavesClick(ClickEvent e) {
        changeView(new SlaveView(HydraApp.this));
    }
    
    @UiHandler("navNodes")
    void onNodesClick(ClickEvent e) {
        changeView(new NodeView(HydraApp.this, null));
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        
        // create a new atmosphere client
        atmosphere = Atmosphere.create();

        // setup JSON Atmosphere connection
        AtmosphereRequestConfig jsonRequestConfig = AtmosphereRequestConfig.create(new ClientSerializer() {
            
            @Override
            public Object deserialize(String message) throws SerializationException {
                // drop invalid messages / extract atmosphere uuid
                if (!message.startsWith("{")) {
                    String[] uuid = message.split("\\|");
                    onAtmosphereRegistered(uuid[0]);

                    return new ClientEvent(EventType.NONE);
                }
                
                try {
                    //WebManager.logger.info("message received: " + message.toString());
                    return ClientEvent.decode(message);
                } catch (java.lang.RuntimeException e) {
                    throw new SerializationException("could not decode " + message.toString());
                }
            }

            @Override
            public String serialize(Object message) throws SerializationException {
                if (message instanceof Event) {
                    return ClientEvent.encode((Event)message);
                }
                else if (message instanceof String) {
                    return (String)message;
                }
                throw new SerializationException("only client event objects are allowed");
            }
            
        });
        
        jsonRequestConfig.setUrl(GWT.getModuleBaseURL() + "atmosphere/view");
        jsonRequestConfig.setContentType("application/json; charset=UTF-8");
        jsonRequestConfig.setTransport(AtmosphereRequestConfig.Transport.STREAMING);
        jsonRequestConfig.setFallbackTransport(AtmosphereRequestConfig.Transport.LONG_POLLING);
        jsonRequestConfig.setTimeout(0);
        jsonRequestConfig.setOpenHandler(new AtmosphereOpenHandler() {
            @Override
            public void onOpen(AtmosphereResponse response) {
                mAlert.close();
            }
        });
        jsonRequestConfig.setCloseHandler(new AtmosphereCloseHandler() {
            @Override
            public void onClose(AtmosphereResponse response) {
                alertColumn.clear();
                mAlert.setType(AlertType.ERROR);
                mAlert.setHeading("Server:");
                mAlert.setText("Disconnected!");
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
            }
        });
        jsonRequestConfig.setMessageHandler(new AtmosphereMessageHandler() {
            @Override
            public void onMessage(AtmosphereResponse response) {
                List<Event> events = response.getMessages();
                for (Event evt : events) {
                    // ignore none events
                    if (evt.equals(EventType.NONE)) continue;
                    
                    // transform to an event object
                    WebManager.logger.info("received event: " + evt.toString());

                    // forward the event to the current view
                    if (currentView != null) {
                        ((EventListener)currentView).onEventRaised(evt);
                    }
                }
            }
        });

        // subscribe to atmosphere channel
        jsonRequest = atmosphere.subscribe(jsonRequestConfig);
    }
    
    protected void onAtmosphereRegistered(String uuid) {
        WebManager.logger.info("atmosphere id: " + uuid.toString());
        
        // store atmosphere uuid for later
        atmosphereId = uuid;
    }
    
    public void subscribeAtmosphere(Long sessionId) {
        if (atmosphereId == null) return;
        
        try {
            jsonRequest.push("subscribe " + atmosphereId + " " + sessionId);
        } catch (SerializationException e) {
            e.printStackTrace();
        }
    }
    
    public void unsubscribeAtmosphere(Long sessionId) {
        if (atmosphereId == null) return;
        
        try {
            jsonRequest.push("unsubscribe " + atmosphereId + " " + sessionId);
        } catch (SerializationException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDetach() {
        // unsubscribe from atmosphere channel
        atmosphere.unsubscribe();
        
        super.onDetach();
    }

    public void changeView(View newView) {
        if (newView == null) {
            newView = new SessionView(HydraApp.this);
        }
        
        if (currentView != null) {
            containerContent.remove(currentView);
        }
        
        currentView = newView;
        containerContent.add((Widget)currentView);
    }
}
