package de.tubs.cs.ibr.hydra.webmanager.client;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Widget;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionWatchView extends View {

    private static SessionWatchViewUiBinder uiBinder = GWT.create(SessionWatchViewUiBinder.class);
    
    @UiField Button buttonBack;

    interface SessionWatchViewUiBinder extends UiBinder<Widget, SessionWatchView> {
    }

    public SessionWatchView(HydraApp app, Session s) {
        super(app);
        initWidget(uiBinder.createAndBindUi(this));
    }

    @Override
    public void eventRaised(Event evt) {
        // TODO Auto-generated method stub
        
    }

    @UiHandler("buttonBack")
    void onClick(ClickEvent e) {
        // switch back to session view
        resetView();
    }
}
