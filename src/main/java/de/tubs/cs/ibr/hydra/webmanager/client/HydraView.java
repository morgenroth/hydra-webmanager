package de.tubs.cs.ibr.hydra.webmanager.client;

import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class HydraView extends Composite {

    private static HydraViewUiBinder uiBinder = GWT.create(HydraViewUiBinder.class);

    interface HydraViewUiBinder extends UiBinder<Widget, HydraView> {
    }
    
    @UiField Container containerContent;
    @UiField NavLink navSession;
    @UiField NavLink navNodes;
    @UiField NavLink navSetup;
    
    Widget currentView = null;

    public HydraView() {
        initWidget(uiBinder.createAndBindUi(this));
        
        // add navigation
        navSession.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (currentView != null)
                    containerContent.remove(currentView);
                
                currentView = new SessionView();
                containerContent.add(currentView);
            }
            
        });
        
        navNodes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (currentView != null)
                    containerContent.remove(currentView);
                
                currentView = new NodeView(null);
                containerContent.add(currentView);
            }
            
        });
        
        currentView = new SessionView();
        containerContent.add(currentView);
    }
}
