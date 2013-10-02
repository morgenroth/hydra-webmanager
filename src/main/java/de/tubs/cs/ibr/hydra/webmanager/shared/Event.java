package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.util.List;


public interface Event {
    public EventType getType();
    public void setType(EventType type);
    public List<EventExtra> getExtras();
    public void setExtras(List<EventExtra> entries);
}
