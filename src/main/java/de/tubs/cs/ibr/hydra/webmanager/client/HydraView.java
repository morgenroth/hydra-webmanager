package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.atmosphere.gwt20.client.Atmosphere;
import org.atmosphere.gwt20.client.AtmosphereCloseHandler;
import org.atmosphere.gwt20.client.AtmosphereMessageHandler;
import org.atmosphere.gwt20.client.AtmosphereOpenHandler;
import org.atmosphere.gwt20.client.AtmosphereRequest;
import org.atmosphere.gwt20.client.AtmosphereRequestConfig;
import org.atmosphere.gwt20.client.AtmosphereResponse;
import org.atmosphere.gwt20.client.ClientSerializer;

import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;

public class HydraView extends Composite {

    private static HydraViewUiBinder uiBinder = GWT.create(HydraViewUiBinder.class);

    private AtmosphereRequest jsonRequest;

    interface HydraViewUiBinder extends UiBinder<Widget, HydraView> {
    }
    
    @UiField Container containerContent;
    @UiField NavLink navSession;
    @UiField NavLink navNodes;
    @UiField NavLink navSetup;
    
    Widget currentView = null;
    Set<EventListener> mEventListener = new HashSet<EventListener>();

    public HydraView() {
        initWidget(uiBinder.createAndBindUi(this));

        Atmosphere atmosphere = Atmosphere.create();

        // setup JSON Atmosphere connection
        AtmosphereRequestConfig jsonRequestConfig = AtmosphereRequestConfig.create(new ClientSerializer() {

            @Override
            public Object deserialize(String message) throws SerializationException {
                return Event.decode(message);
            }

            @Override
            public String serialize(Object message) throws SerializationException {
                if (message instanceof Event) {
                    return Event.encode((Event)message);
                }
                return message.toString();
            }
            
        });
        
        jsonRequestConfig.setUrl(GWT.getModuleBaseURL() + "atmosphere/view");
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
                    WebManager.logger.info("received message: " + event.toString());

                    for (EventListener l : mEventListener) {
                        l.eventRaised(event);
                    }
                }
            }
        });

        // subscribe to atmosphere channel
        jsonRequest = atmosphere.subscribe(jsonRequestConfig);
        
        // add navigation
        navSession.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (currentView != null) {
                    containerContent.remove(currentView);
                    mEventListener.remove(currentView);
                }
                
                currentView = new SessionView();
                containerContent.add(currentView);
                mEventListener.add((EventListener)currentView);
            }
            
        });
        
        navNodes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (currentView != null) {
                    containerContent.remove(currentView);
                    mEventListener.remove(currentView);
                }
                
                currentView = new NodeView(null);
                containerContent.add(currentView);
            }
            
        });
        
        currentView = new SessionView();
        containerContent.add(currentView);
        mEventListener.add((EventListener)currentView);
    }
}
