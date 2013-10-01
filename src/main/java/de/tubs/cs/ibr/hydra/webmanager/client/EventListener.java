package de.tubs.cs.ibr.hydra.webmanager.client;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;

public interface EventListener {
    public void eventRaised(Event evt);
}
