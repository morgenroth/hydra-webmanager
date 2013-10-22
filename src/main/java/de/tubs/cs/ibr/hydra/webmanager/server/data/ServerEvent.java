package de.tubs.cs.ibr.hydra.webmanager.server.data;

import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventData;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventDataExtra;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventDataFactory;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;

public class ServerEvent extends Event {
    
    public ServerEvent(EventData data) {
        super(data);
    }
    
    public ServerEvent(EventType t) {
        super(t);
    }

    @Override
    protected EventDataExtra createExtra() {
        // create a new extra data
        EventDataFactory factory = AutoBeanFactorySource.create(EventDataFactory.class);
        return factory.extra().as();
    }

    @Override
    protected EventData createEventData() {
        EventDataFactory factory = AutoBeanFactorySource.create(EventDataFactory.class);
        return factory.event().as();
    }
}
