package de.tubs.cs.ibr.hydra.webmanager.client;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Widget;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionEditView extends View {

    private static SessionEditViewUiBinder uiBinder = GWT.create(SessionEditViewUiBinder.class);
    
    @UiField Button buttonBack;
    @UiField NavLink navProperties;
    @UiField NavLink navImages;
    @UiField NavLink navBase;
    @UiField NavLink navSetup;
    @UiField NavLink navMovement;
    @UiField NavLink navNodes;
    
    @UiField DeckPanel panelMain;
    
    @UiField TextBox textPropDesc;
    @UiField TextBox textPropOwner;
    
    NavLink activeNavLink = null;
    Session session = null;
    
    interface SessionEditViewUiBinder extends UiBinder<Widget, SessionEditView> {
    }

    public SessionEditView(HydraApp app, Session s) {
        super(app);
        session = s;
        initWidget(uiBinder.createAndBindUi(this));
        
        // load session properties
        refreshSessionProperties();
        
        // show properties as default
        navigateTo(navProperties, 0);
    }
    
    private void refreshSessionProperties() {
        if (session == null) return;
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getSession(session.id, new AsyncCallback<Session>() {
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Can not get session properties.");
                resetView();
            }

            @Override
            public void onSuccess(Session result) {
                textPropDesc.setText(result.name);
                textPropOwner.setText(result.username);
                textPropOwner.setEnabled(false);
            }
        });
    }

    @Override
    public void eventRaised(Event evt) {
        // no dynamic updates here
    }
    
    private void navigateTo(NavLink link, int index) {
        if (activeNavLink != null) activeNavLink.setActive(false);
        if (link != null) link.setActive(true);
        
        activeNavLink = link;
        panelMain.showWidget(index);
    }

    @UiHandler("buttonBack")
    void onClickBack(ClickEvent e) {
        // switch back to session view
        resetView();
    }
    
    @UiHandler("navProperties")
    void onClickProperties(ClickEvent e) {
        navigateTo(navProperties, 0);
    }
    
    @UiHandler("navImages")
    void onClickImages(ClickEvent e) {
        navigateTo(navImages, 1);
    }
    
    @UiHandler("navBase")
    void onClickBase(ClickEvent e) {
        navigateTo(navBase, 2);
    }
    
    @UiHandler("navSetup")
    void onClickSetup(ClickEvent e) {
        navigateTo(navSetup, 3);
    }
    
    @UiHandler("navMovement")
    void onClickMovement(ClickEvent e) {
        navigateTo(navMovement, 4);
    }
    
    @UiHandler("navNodes")
    void onClickNodes(ClickEvent e) {
        navigateTo(navNodes, 5);
    }
}
