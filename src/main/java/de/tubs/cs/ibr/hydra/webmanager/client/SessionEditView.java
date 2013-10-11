package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;

import com.github.gwtbootstrap.client.ui.Alert;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FileUpload;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.TabPanel.ShownEvent;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Widget;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventExtra;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet.MobilityModel;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionEditView extends View {

    private static SessionEditViewUiBinder uiBinder = GWT.create(SessionEditViewUiBinder.class);
    
    @UiField Button buttonBack;
    
    @UiField TabPanel panelTabs;
    @UiField Tab tabNodes;
    
    @UiField Column columnFormActions;
    @UiField Button buttonApply;
    @UiField Button buttonReset;
    @UiField Column alertColumn;
    
    @UiField TextBox textPropKey;
    @UiField TextBox textPropDesc;
    @UiField TextBox textPropOwner;
    @UiField TextBox textPropState;
    @UiField CheckBox checkRemove;
    @UiField Button buttonRemove;
    
    @UiField ListBox listBaseImage;
    @UiField TextBox textBaseRepository;
    @UiField TextArea textBasePackages;
    @UiField TextArea textBaseMonitorNodes;
    @UiField TextArea textBaseQemuTemplate;
    @UiField TextArea textBaseVboxTemplate;
    
    @UiField ListBox listMovementAlgorithm;
    @UiField DeckPanel panelMovement;
    
    // RWP
    @UiField TextBox textMovementRwpDuration;
    @UiField TextBox textMovementRwpAreaSizeHeight;
    @UiField TextBox textMovementRwpAreaSizeWidth;
    @UiField TextBox textMovementRwpResolution;
    @UiField TextBox textMovementRwpMovetime;
    @UiField TextBox textMovementRwpVmin;
    @UiField TextBox textMovementRwpVmax;
    @UiField TextBox textMovementRwpRange;
    
    // ONE
    @UiField FileUpload textMovementOneUpload;
    
    // STATIC
    @UiField TextArea textMovementStaticConnections;
    
    @UiField SessionNodesEditor nodesEditor;
    
    NavLink activeNavLink = null;
    Session mSession = null;
    Session mChangedSession = null;
    
    final Alert mAlert = new Alert();
    
    interface SessionEditViewUiBinder extends UiBinder<Widget, SessionEditView> {
    }

    public SessionEditView(HydraApp app, Session s) {
        super(app);
        initWidget(uiBinder.createAndBindUi(this));
        
        if (s == null) {
            // create a session first
            createSession();
        } else {
            // initialize the session directly
            init(s);
        }
    }
    
    private void refresh() {
        if (mSession == null) return;
        init(mSession);
    }
    
    private void init(Session s) {
        // load session properties
        refreshSessionProperties(s);
        
        // load nodes
        nodesEditor.refresh(s);
    }
    
    private void refreshSessionProperties(Session session) {
        // check for null session objects
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
                setSession(result);
            }
        });
    }
    
    private void refreshSessionImages(final String selection) {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getAvailableImages(new AsyncCallback<ArrayList<String>>() {
            @Override
            public void onFailure(Throwable caught) {
                Window.alert("Can not get session images.");
                resetView();
            }

            @Override
            public void onSuccess(ArrayList<String> images) {
                listBaseImage.clear();
                
                // add selection hint
                listBaseImage.addItem("- please select an image -");
                
                for (String i : images) {
                    listBaseImage.addItem(i);
                }
                
                if (selection == null) {
                    listBaseImage.setSelectedIndex(0);
                } else {
                    listBaseImage.setSelectedValue(selection);
                }
            }
        });
    }
    
    private void setSession(Session result) {
        // reset changes
        mChangedSession = new Session(result.id);
        
        // store data locally
        mSession = result;

        // load properties
        textPropKey.setText(result.id.toString());
        textPropDesc.setText(result.name);
        textPropOwner.setText(result.username);
        textPropState.setText(result.state.toString());
        
        // load base
        textBaseRepository.setText(result.repository);
        textBasePackages.setText(result.packages);
        textBaseMonitorNodes.setText(result.monitor_nodes);
        textBaseQemuTemplate.setText(result.qemu_template);
        textBaseVboxTemplate.setText(result.vbox_template);
        
        // load session images
        refreshSessionImages(result.image);
        
        // show movement algorithm
        if ((result.mobility != null) && (result.mobility.model != null))
            listMovementAlgorithm.setSelectedValue(result.mobility.model.toString());
        
        onMovementAlgorithmChanged(null);
    }
    
    private void createSession() {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.createSession(new AsyncCallback<Session>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Session result) {
                init(result);
            }
        });
    }
    
    @Override
    public void eventRaised(Event evt) {
        // do not update, if we don't have a session
        if (mSession == null) return;
        
        // refresh table on refresh event
        if (EventType.NODE_STATE_CHANGED.equals(evt)) {
            if (isRelated(evt)) {
                // refresh nodes
                nodesEditor.refresh(mSession);
            }
        }
        else if (EventType.SESSION_REMOVED.equals(evt)) {
            if (isRelated(evt)) {
                // close current view
                resetView();
            }
        }
        else if (EventType.SESSION_STATE_CHANGED.equals(evt)) {
            if (isRelated(evt)) {
                refresh();
            }
        }
        else if (EventType.SESSION_DATA_UPDATED.equals(evt)) {
            if (isRelated(evt)) {
                refresh();
            }
        }
        else if (EventType.SLAVE_STATE_CHANGED.equals(evt)) {
         // refresh nodes
            nodesEditor.refresh(mSession);
        }
    }
    
    private boolean isRelated(Event evt) {
        if (evt.getExtras() == null) return true;
        
        for (EventExtra e : evt.getExtras()) {
            if (EventType.EXTRA_SESSION_ID.equals(e.getKey())) {
                if (mSession.id.toString().equals(e.getData())) {
                    return true;
                }
            }
        }
        return false;
    }

    @UiHandler("buttonBack")
    void onClickBack(ClickEvent e) {
        // switch back to session view
        resetView();
    }
    
    @UiHandler("checkRemove")
    void onRemoveLockChanged(ClickEvent e) {
        buttonRemove.setEnabled(checkRemove.isChecked());
    }
    
    @UiHandler("buttonRemove")
    void onRemoveSession(ClickEvent e) {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.triggerAction(mSession, Session.Action.REMOVE, new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable caught) {
                if (mAlert.getElement().hasParentElement())
                    mAlert.removeFromParent();
                
                mAlert.setType(AlertType.ERROR);
                mAlert.setText("Failure! Can not remove this session.");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
            }

            @Override
            public void onSuccess(Void result) {
                if (mAlert.getElement().hasParentElement())
                    mAlert.removeFromParent();
                
                mAlert.setType(AlertType.SUCCESS);
                mAlert.setText("Successful removed!");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
                
                // now the removal event will close this view
            }
            
        });
    }
    
    @UiHandler("buttonApply")
    void onPropertiesApply(ClickEvent e) {
        // do not apply uninitialized changes
        if (mChangedSession == null) return;
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.applySession(mChangedSession, new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable caught) {
                alertColumn.clear();
                mAlert.setType(AlertType.ERROR);
                mAlert.setText("Failure! Can not apply session changes.");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
            }

            @Override
            public void onSuccess(Void result) {
                alertColumn.clear();
                mAlert.setType(AlertType.SUCCESS);
                mAlert.setText("Successful saved!");
                mAlert.setClose(true);
                mAlert.setAnimation(true);
                alertColumn.add(mAlert);
                
                scheduleAlertClear(mAlert, 5000);
            }
            
        });
    }
    
    private void scheduleAlertClear(final Alert alert, int delay) {
        Timer t = new Timer() {
            @Override
            public void run() {
                alert.close();
            }
        };
        
        t.schedule(delay);
    }
    
    @UiHandler("buttonReset")
    void onPropertiesReset(ClickEvent e) {
        refresh();
    }
    
    @UiHandler("textPropDesc")
    void onDescriptionChanged(ChangeEvent evt) {
        mChangedSession.name = textPropDesc.getText();
    }
    
    @UiHandler("listBaseImage")
    void onBaseImageChanged(ChangeEvent evt) {
        mChangedSession.image = listBaseImage.getValue();
    }
    
    @UiHandler("textBaseRepository")
    void onBaseRepositoryChanged(ChangeEvent evt) {
        mChangedSession.repository = textBaseRepository.getText();
    }
    
    @UiHandler("textBasePackages")
    void onBasePackagesChanged(ChangeEvent evt) {
        mChangedSession.packages = textBasePackages.getText();
    }
    
    @UiHandler("textBaseMonitorNodes")
    void onBaseMonitorNodesChanged(ChangeEvent evt) {
        mChangedSession.monitor_nodes = textBaseMonitorNodes.getText();
    }
    
    @UiHandler("textBaseQemuTemplate")
    void onBaseQemuTemplateChanged(ChangeEvent evt) {
        mChangedSession.qemu_template = textBaseQemuTemplate.getText();
    }
    
    @UiHandler("textBaseVboxTemplate")
    void onBaseVboxTemplateChanged(ChangeEvent evt) {
        mChangedSession.vbox_template = textBaseVboxTemplate.getText();
    }
    
    @UiHandler("listMovementAlgorithm")
    void onMovementAlgorithmChanged(ChangeEvent evt) {
        panelMovement.showWidget(listMovementAlgorithm.getSelectedIndex());
        
        MobilityParameterSet m = new MobilityParameterSet();
        m.model = MobilityModel.fromString(listMovementAlgorithm.getValue());
        mChangedSession.mobility = m;
        
        // load parameters
        switch (m.model) {
            case RANDOM_WALK:
                if (mSession.mobility.parameters.containsKey("duration")) {
                    textMovementRwpDuration.setText(mSession.mobility.parameters.get("duration"));
                } else {
                    textMovementRwpDuration.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("height")) {
                    textMovementRwpAreaSizeHeight.setText(mSession.mobility.parameters.get("height"));
                } else {
                    textMovementRwpAreaSizeHeight.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("width")) {
                    textMovementRwpAreaSizeWidth.setText(mSession.mobility.parameters.get("width"));
                } else {
                    textMovementRwpAreaSizeWidth.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("resolution")) {
                    textMovementRwpResolution.setText(mSession.mobility.parameters.get("resolution"));
                } else {
                    textMovementRwpResolution.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("movetime")) {
                    textMovementRwpMovetime.setText(mSession.mobility.parameters.get("movetime"));
                } else {
                    textMovementRwpMovetime.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("vmin")) {
                    textMovementRwpVmin.setText(mSession.mobility.parameters.get("vmin"));
                } else {
                    textMovementRwpVmin.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("vmax")) {
                    textMovementRwpVmax.setText(mSession.mobility.parameters.get("vmax"));
                } else {
                    textMovementRwpVmax.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("range")) {
                    textMovementRwpRange.setText(mSession.mobility.parameters.get("range"));
                } else {
                    textMovementRwpRange.setText(null);
                }
                break;
            case STATIC:
                if (mSession.mobility.parameters.containsKey("connections")) {
                    textMovementStaticConnections.setText(mSession.mobility.parameters.get("connections"));
                } else {
                    textMovementStaticConnections.setText(null);
                }
                break;
            case THE_ONE:
                if (mSession.mobility.parameters.containsKey("file")) {
                    textMovementOneUpload.setText(mSession.mobility.parameters.get("file"));
                } else {
                    textMovementOneUpload.setText(null);
                }
                break;
            default:
                break;
        }
    }
    
    // RWP
    @UiHandler("textMovementRwpDuration")
    void onMovementRwpDurationChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("duration", textMovementRwpDuration.getText());
    }
    
    @UiHandler("textMovementRwpAreaSizeHeight")
    void onMovementRwpAreaSizeHeightChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("height", textMovementRwpAreaSizeHeight.getText());
    }
    
    @UiHandler("textMovementRwpAreaSizeWidth")
    void onMovementRwpAreaSizeWidthChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("width", textMovementRwpAreaSizeWidth.getText());
    }
    
    @UiHandler("textMovementRwpResolution")
    void onMovementRwpResolutionChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("resolution", textMovementRwpResolution.getText());
    }
    
    @UiHandler("textMovementRwpMovetime")
    void onMovementRwpMovetimeChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("movetime", textMovementRwpMovetime.getText());
    }
    
    @UiHandler("textMovementRwpVmin")
    void onMovementRwpVminChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("vmin", textMovementRwpVmin.getText());
    }
    
    @UiHandler("textMovementRwpVmax")
    void onMovementRwpVmaxChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("vmax", textMovementRwpVmax.getText());
    }
    
    @UiHandler("textMovementRwpRange")
    void onMovementRwpRangeChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("range", textMovementRwpRange.getText());
    }
    
    // ONE
    @UiHandler("textMovementOneUpload")
    void onMovementOneUploadChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("file", textMovementOneUpload.getText());
    }
    
    // STATIC
    @UiHandler("textMovementStaticConnections")
    void onMovementStaticConnectionsChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("connections", textMovementStaticConnections.getText());
    }
    
    @UiHandler("panelTabs")
    void onTabChange(ShownEvent event) {
        if (tabNodes.isActive()) {
            columnFormActions.setVisible(false);
        } else {
            columnFormActions.setVisible(true);
        }
    }
}
