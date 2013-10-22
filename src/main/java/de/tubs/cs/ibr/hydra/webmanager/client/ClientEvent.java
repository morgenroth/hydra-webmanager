package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.gwt.core.client.GWT;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventData;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventDataExtra;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventDataFactory;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;

public class ClientEvent extends Event {
    
    public ClientEvent(EventData data) {
        super(data);
    }
    
    public ClientEvent(EventType t) {
        super(t);
    }

    @Override
    protected EventDataExtra createExtra() {
        // create a new extra data
        EventDataFactory factory = GWT.create(EventDataFactory.class);
        return factory.extra().as();
    }

    @Override
    protected EventData createEventData() {
        EventDataFactory factory = GWT.create(EventDataFactory.class);
        return factory.event().as();
    }
    
    public static Event decode(String message) {
        EventDataFactory factory = GWT.create(EventDataFactory.class);
        AutoBean<EventData> bean = AutoBeanCodex.decode(factory, EventData.class, message);
        return new ClientEvent(bean.as());
    }
    
    public static String encode(Event evt) {
        AutoBean<EventData> bean = AutoBeanUtils.getAutoBean(evt.getEventData());
        return AutoBeanCodex.encode(bean).getPayload();
    }
}
