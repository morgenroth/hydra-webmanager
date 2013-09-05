package de.tubs.cs.ibr.hydra.webmanager.client;

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
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;

public class SessionView extends Composite implements HasText {

    private static SessionViewUiBinder uiBinder = GWT.create(SessionViewUiBinder.class);
    
    private MyBeanFactory beanFactory = GWT.create(MyBeanFactory.class);
    
    private AtmosphereRequest jsonRequest;

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
                }
            }
        });
        
        jsonRequest = atmosphere.subscribe(jsonRequestConfig);
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
        myevent.setData("Hello World!");
        
        try {
            jsonRequest.push(myevent);
        } catch (SerializationException ex) {
            WebManager.logger.log(Level.SEVERE, "Failed to serialize message", ex);
        }
        
        Window.alert("Hello!");
    }

    public void setText(String text) {
        button.setText(text);
    }

    public String getText() {
        return button.getText();
    }

}
