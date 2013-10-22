package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.gwt.user.client.ui.Composite;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;

public abstract class View extends Composite implements EventListener {
    private HydraApp mApp;
    
    protected View(HydraApp app) {
        mApp = app;
    }
    
    protected void changeView(View newView) {
        mApp.changeView(newView);
    }
    
    protected HydraApp getApplication() {
        return mApp;
    }
    
    protected void resetView() {
        mApp.changeView(null);
    }

    @Override
    public abstract void onEventRaised(Event evt);
}
