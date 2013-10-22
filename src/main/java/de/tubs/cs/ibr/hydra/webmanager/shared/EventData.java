package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.util.List;


public interface EventData {
    public EventType getType();
    public void setType(EventType type);
    public List<EventDataExtra> getExtras();
    public void setExtras(List<EventDataExtra> entries);
}
