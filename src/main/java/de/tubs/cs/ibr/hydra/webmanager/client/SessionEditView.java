package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;

import com.github.gwtbootstrap.client.ui.Alert;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FileUpload;
import com.github.gwtbootstrap.client.ui.FormActions;
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
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Widget;

import de.tubs.cs.ibr.hydra.webmanager.shared.Credentials;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataFile;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet.MobilityModel;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionEditView extends View {

    private static SessionEditViewUiBinder uiBinder = GWT.create(SessionEditViewUiBinder.class);

    private static Credentials mCredentials = null;
    
    @UiField TabPanel panelTabs;
    @UiField Tab tabNodes;
    
    @UiField FormActions faNodes;
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
    @UiField TextArea textBaseInstall;
    
    @UiField TextArea textBaseQemuTemplate;
    @UiField TextArea textBaseVboxTemplate;
    
    @UiField TextArea textBaseSetupScript;
    @UiField TextArea textBaseIndividualSetupScript;
    
    @UiField ListBox listBasePackages;
    @UiField Button buttonBasePackages;
    @UiField FormPanel formBasePackages;
    @UiField FileUpload uploadBasePackages;
    
    @UiField TextBox textNetworkNodeAddressMin;
    @UiField TextBox textNetworkNodeAddressMax;
    @UiField TextBox textNetworkNodeNetmask;
    @UiField TextArea textNetworkMonitorNodes;
    
    @UiField TextBox textStatsInterval;
    @UiField CheckBox checkStatsRecordContact;
    @UiField CheckBox checkStatsRecordMovement;
    
    @UiField TextBox textSimulationResolution;
    @UiField TextBox textSimulationRange;
    
    @UiField ListBox listMovementAlgorithm;
    @UiField DeckPanel panelMovement;
    
    // RWP
    @UiField TextBox textMovementRwpDuration;
    @UiField TextBox textMovementRwpAreaSizeHeight;
    @UiField TextBox textMovementRwpAreaSizeWidth;
    @UiField TextBox textMovementRwpWaittime;
    @UiField TextBox textMovementRwpVmin;
    @UiField TextBox textMovementRwpVmax;
    @UiField TextBox textMovementRwpCoordLat;
    @UiField TextBox textMovementRwpCoordLng;
    
    // RW
    @UiField TextBox textMovementRwDuration;
    @UiField TextBox textMovementRwAreaSizeHeight;
    @UiField TextBox textMovementRwAreaSizeWidth;
    @UiField TextBox textMovementRwMovetime;
    @UiField TextBox textMovementRwVmin;
    @UiField TextBox textMovementRwVmax;
    @UiField TextBox textMovementRwCoordLat;
    @UiField TextBox textMovementRwCoordLng;
    
    // TRACE
    @UiField FormPanel formMovementTrace;
    @UiField FileUpload uploadMovementTrace;
    @UiField ListBox listMovementTrace;
    @UiField Button buttonMovementTrace;
    @UiField CheckBox checkMovementTraceRepetition;
    
    // STATIC
    @UiField TextArea textMovementStaticPositions;
    @UiField TextBox textMovementStaticDuration;
    @UiField TextBox textMovementStaticCoordLat;
    @UiField TextBox textMovementStaticCoordLng;
    
    @UiField SessionNodesEditor nodesEditor;
    
    NavLink activeNavLink = null;
    Session mSession = null;
    Session mChangedSession = null;
    
    final Alert mAlert = new Alert();
    
    interface SessionEditViewUiBinder extends UiBinder<Widget, SessionEditView> {
    }

    public SessionEditView(HydraApp app, Session s) throws UnauthorizedException {
        super(app);
        initWidget(uiBinder.createAndBindUi(this));
        
        mCredentials = app.getCredentials();
        if ( mCredentials == null )
            throw new UnauthorizedException();
        
        if (s == null) {
            // create a session first
            createSession(mCredentials.getUsername());
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
        
        // load data files
        refreshDataFiles(s);
        
        // load nodes
        nodesEditor.refresh(s);
    }
    
    private void refreshDataFiles(Session session) {
        // check for null session objects
        if (session == null) return;
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getSessionFiles(session, "data", new AsyncCallback<ArrayList<DataFile>>() {
            
            @Override
            public void onSuccess(ArrayList<DataFile> result) {
                listMovementTrace.clear();
                listMovementTrace.addItem("- no file selected -", "");
                listMovementTrace.setSelectedIndex(0);
                
                for (DataFile f : result) {
                    listMovementTrace.addItem(f.filename);
                }
                
                if (result.size() > 0) {
                    listMovementTrace.setEnabled(true);
                    
                    String selectedTraceFile = null;
                    
                    if ((mChangedSession != null) && (mChangedSession.mobility != null) && (mChangedSession.mobility.parameters != null))
                    {
                        if (mChangedSession.mobility.parameters.containsKey("tracefile")) {
                            selectedTraceFile = mChangedSession.mobility.parameters.get("tracefile");
                        }
                    }
                    
                    if ((selectedTraceFile == null) && (mChangedSession != null) && (mSession.mobility != null) && (mSession.mobility.parameters != null))
                    {
                        if (mSession.mobility.parameters.containsKey("tracefile")) {
                            selectedTraceFile = mSession.mobility.parameters.get("tracefile");
                        }
                    }
                    
                    if (selectedTraceFile != null) {
                        listMovementTrace.setSelectedValue(selectedTraceFile);
                        buttonMovementTrace.setEnabled(!listMovementTrace.getValue().isEmpty());
                    }
                } else {
                    buttonMovementTrace.setEnabled(false);
                    listMovementTrace.setEnabled(false);
                }
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // error
            }
        });
        
        mcs.getSessionFiles(session, "packages", new AsyncCallback<ArrayList<DataFile>>() {
            
            @Override
            public void onSuccess(ArrayList<DataFile> result) {
                listBasePackages.clear();
                
                for (DataFile f : result) {
                    listBasePackages.addItem(f.filename);
                }
                
                if (result.size() > 0) {
                    listBasePackages.setEnabled(true);
                } else {
                    buttonBasePackages.setEnabled(false);
                    listBasePackages.setEnabled(false);
                }
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // error
            }
        });
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
        textBaseInstall.setText(result.packages);
        textBaseQemuTemplate.setText(result.qemu_template);
        textBaseVboxTemplate.setText(result.vbox_template);
        textBaseSetupScript.setText(result.script_generic);
        textBaseIndividualSetupScript.setText(result.script_individual);
        
        // load global parameters
        if (mSession.range != null) {
            textSimulationRange.setText(mSession.range.toString());
        } else {
            textSimulationRange.setText("");
        }
        
        if (mSession.resolution != null) {
            textSimulationResolution.setText(mSession.resolution.toString());
        } else {
            textSimulationResolution.setText("");
        }
        
        // load network
        textNetworkNodeAddressMax.setText(result.maxaddr);
        textNetworkNodeAddressMin.setText(result.minaddr);
        textNetworkNodeNetmask.setText(result.netmask);
        textNetworkMonitorNodes.setText(result.monitor_nodes);
        
        // load stats
        if (result.stats_interval != null) {
            textStatsInterval.setText(result.stats_interval.toString());
        } else {
            textStatsInterval.setText("");
        }
        
        checkStatsRecordContact.setValue(result.stats_record_contact == null ? false : result.stats_record_contact);
        checkStatsRecordMovement.setValue(result.stats_record_movement == null ? false : result.stats_record_movement);
        
        // load session images
        refreshSessionImages(result.image);
        
        // show movement algorithm
        if ((result.mobility != null) && (result.mobility.model != null))
            listMovementAlgorithm.setSelectedValue(result.mobility.model.toString());
        
        onMovementAlgorithmChanged(null);
    }
    
    private void createSession(String username) {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.createSession(mCredentials, new AsyncCallback<Session>() {
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
    public void onEventRaised(Event evt) {
        // do not update, if we don't have a session
        if (mSession == null) return;
        
        // refresh table on refresh event
        if (evt.equals(EventType.NODE_STATE_CHANGED)) {
            if (isRelated(evt)) {
                // refresh nodes
                nodesEditor.refresh(mSession);
            }
        }
        else if (evt.equals(EventType.SESSION_REMOVED)) {
            if (isRelated(evt)) {
                // close current view
                resetView();
            }
        }
        else if (evt.equals(EventType.SESSION_STATE_CHANGED)) {
            if (isRelated(evt)) {
                refresh();
            }
        }
        else if (evt.equals(EventType.SESSION_DATA_UPDATED)) {
            if (isRelated(evt)) {
                refresh();
            }
        }
        else if (evt.equals(EventType.SLAVE_STATE_CHANGED)) {
         // refresh nodes
            nodesEditor.refresh(mSession);
        }
    }
    
    private boolean isRelated(Event evt) {
        // get session id (null if not set)
        Long session_id = evt.getExtraLong(EventType.EXTRA_SESSION_ID);
        
        // check if session id is set
        if (session_id == null) return false;
        
        // compare to local session id
        return session_id.equals(mSession.id);
    }

    @UiHandler("checkRemove")
    void onRemoveLockChanged(ClickEvent e) {
        buttonRemove.setEnabled(checkRemove.getValue());
    }
    
    @UiHandler("buttonRemove")
    void onRemoveSession(ClickEvent e) {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.triggerAction(mSession, Session.Action.REMOVE, mCredentials, new AsyncCallback<Void>() {

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
                mAlert.setText("Successfully removed!");
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
        mcs.applySession(mChangedSession, mCredentials, new AsyncCallback<Void>() {

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
                mAlert.setText("Successfully saved!");
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
    
    @UiHandler("textBaseInstall")
    void onBaseInstallChanged(ChangeEvent evt) {
        mChangedSession.packages = textBaseInstall.getText();
    }
    
    @UiHandler("textNetworkNodeAddressMax")
    void onNetworkAddressChanged(ChangeEvent evt) {
        mChangedSession.maxaddr = textNetworkNodeAddressMax.getText();
        mChangedSession.minaddr = textNetworkNodeAddressMin.getText();
        mChangedSession.netmask = textNetworkNodeNetmask.getText();
    }
    
    @UiHandler("textNetworkNodeAddressMin")
    void onNetworkMinAddressChanged(ChangeEvent evt) {
        onNetworkAddressChanged(evt);
    }
    
    @UiHandler("textNetworkNodeNetmask")
    void onNetworkNetmaskChanged(ChangeEvent evt) {
        onNetworkAddressChanged(evt);
    }

    @UiHandler("textNetworkMonitorNodes")
    void onNetworkMonitorNodesChanged(ChangeEvent evt) {
        mChangedSession.monitor_nodes = textNetworkMonitorNodes.getText();
    }
    
    @UiHandler("textBaseQemuTemplate")
    void onBaseQemuTemplateChanged(ChangeEvent evt) {
        mChangedSession.qemu_template = textBaseQemuTemplate.getText();
    }
    
    @UiHandler("textBaseVboxTemplate")
    void onBaseVboxTemplateChanged(ChangeEvent evt) {
        mChangedSession.vbox_template = textBaseVboxTemplate.getText();
    }
    
    @UiHandler("textBaseSetupScript")
    void onBaseSetupScriptChanged(ChangeEvent evt) {
        mChangedSession.script_generic = textBaseSetupScript.getText();
    }
    
    @UiHandler("textBaseIndividualSetupScript")
    void onBaseIndividualSetupScriptChanged(ChangeEvent evt) {
        mChangedSession.script_individual = textBaseIndividualSetupScript.getText();
    }
    
    @UiHandler("buttonBasePackages")
    void onBasePackagesButtonClicked(ClickEvent e) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("tracefile", "");
        
        buttonBasePackages.setEnabled(false);
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.removeSessionFile(mSession, "packages", listBasePackages.getValue(), mCredentials, new AsyncCallback<Void>() {
            
            @Override
            public void onSuccess(Void result) {
                refreshDataFiles(mSession);
            }
            
            @Override
            public void onFailure(Throwable caught) {
            }
        });
    }
    
    @UiHandler("formBasePackages")
    void onBasePackagesSubmit(FormPanel.SubmitEvent event) {
        // TODO: show progress
        formBasePackages.setVisible(false);
    }
    
    @UiHandler("formBasePackages")
    void onBasePackagesUploadCompleted(FormPanel.SubmitCompleteEvent event) {
        uploadBasePackages.setText(null);
        formBasePackages.setVisible(true);
        
        // refresh file listing
        refreshDataFiles(mSession);
    }
    
    @UiHandler("uploadBasePackages")
    void onBasePackagesChanged(ChangeEvent evt) {
        formBasePackages.setEncoding(FormPanel.ENCODING_MULTIPART);
        formBasePackages.setMethod(FormPanel.METHOD_POST);
        formBasePackages.setAction(GWT.getModuleBaseURL() + "upload?sid=" + mSession.id);
        formBasePackages.submit();
    }
    
    @UiHandler("listBasePackages")
    void onBasePackagesFileChanged(ChangeEvent evt) {
        buttonBasePackages.setEnabled(!listBasePackages.getValue().isEmpty());
    }
    
    @UiHandler("textStatsInterval")
    void onStatsIntervalChanged(ChangeEvent evt) {
        String value = textStatsInterval.getText();
        mChangedSession.stats_interval = (value.length() == 0) ? 0 : Long.valueOf(value);
    }
    
    @UiHandler("checkStatsRecordContact")
    void onStatsRecordContactChanged(ClickEvent e) {
        mChangedSession.stats_record_contact = checkStatsRecordContact.getValue();
    }
    
    @UiHandler("checkStatsRecordMovement")
    void onStatsRecordMovementChanged(ClickEvent e) {
        mChangedSession.stats_record_movement = checkStatsRecordMovement.getValue();
    }
    
    @UiHandler("listMovementAlgorithm")
    void onMovementAlgorithmChanged(ChangeEvent evt) {
        panelMovement.showWidget(listMovementAlgorithm.getSelectedIndex());
        
        MobilityParameterSet m = new MobilityParameterSet();
        m.model = MobilityModel.fromString(listMovementAlgorithm.getValue());
        mChangedSession.mobility = m;
        
        // load parameters
        switch (m.model) {
            case RANDOM_WAYPOINT:
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
                
                if (mSession.mobility.parameters.containsKey("waittime")) {
                    textMovementRwpWaittime.setText(mSession.mobility.parameters.get("waittime"));
                } else {
                	textMovementRwpWaittime.setText(null);
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
                
                if (mSession.mobility.parameters.containsKey("lat")) {
                    textMovementRwpCoordLat.setText(mSession.mobility.parameters.get("lat"));
                } else {
                    textMovementRwpCoordLat.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("lng")) {
                    textMovementRwpCoordLng.setText(mSession.mobility.parameters.get("lng"));
                } else {
                    textMovementRwpCoordLng.setText(null);
                }
                break;
            case RANDOM_WALK:
                if (mSession.mobility.parameters.containsKey("duration")) {
                    textMovementRwDuration.setText(mSession.mobility.parameters.get("duration"));
                } else {
                    textMovementRwDuration.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("height")) {
                    textMovementRwAreaSizeHeight.setText(mSession.mobility.parameters.get("height"));
                } else {
                    textMovementRwAreaSizeHeight.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("width")) {
                    textMovementRwAreaSizeWidth.setText(mSession.mobility.parameters.get("width"));
                } else {
                    textMovementRwAreaSizeWidth.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("movetime")) {
                    textMovementRwMovetime.setText(mSession.mobility.parameters.get("movetime"));
                } else {
                    textMovementRwMovetime.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("vmin")) {
                    textMovementRwVmin.setText(mSession.mobility.parameters.get("vmin"));
                } else {
                    textMovementRwVmin.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("vmax")) {
                    textMovementRwVmax.setText(mSession.mobility.parameters.get("vmax"));
                } else {
                    textMovementRwVmax.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("lat")) {
                    textMovementRwCoordLat.setText(mSession.mobility.parameters.get("lat"));
                } else {
                    textMovementRwCoordLat.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("lng")) {
                    textMovementRwCoordLng.setText(mSession.mobility.parameters.get("lng"));
                } else {
                    textMovementRwCoordLng.setText(null);
                }
                break;
            case STATIC:
                if (mSession.mobility.parameters.containsKey("positions")) {
                    textMovementStaticPositions.setText(mSession.mobility.parameters.get("positions"));
                } else {
                    textMovementStaticPositions.setText(null);
                }
                if (mSession.mobility.parameters.containsKey("duration")) {
                    textMovementStaticDuration.setText(mSession.mobility.parameters.get("duration"));
                } else {
                    textMovementStaticDuration.setText(null);
                }
                if (mSession.mobility.parameters.containsKey("lat")) {
                    textMovementStaticCoordLat.setText(mSession.mobility.parameters.get("lat"));
                } else {
                    textMovementStaticCoordLat.setText(null);
                }
                
                if (mSession.mobility.parameters.containsKey("lng")) {
                    textMovementStaticCoordLng.setText(mSession.mobility.parameters.get("lng"));
                } else {
                    textMovementStaticCoordLng.setText(null);
                }
                break;
            case TRACE:
                if (mSession.mobility.parameters.containsKey("tracefile")) {
                    listMovementTrace.setSelectedValue(mSession.mobility.parameters.get("tracefile"));
                    buttonMovementTrace.setEnabled(true);
                } else {
                    listMovementTrace.setSelectedIndex(0);
                    buttonMovementTrace.setEnabled(false);
                }
                
                if (mSession.mobility.parameters.containsKey("repeat")) {
                    checkMovementTraceRepetition.setValue("yes".equals(mSession.mobility.parameters.get("repeat")));
                } else {
                    checkMovementTraceRepetition.setValue(false);
                }
                break;
            default:
                break;
        }
    }
    
    @UiHandler("textSimulationResolution")
    void onSimulationResolutionChanged(ChangeEvent evt) {
        try {
            mChangedSession.resolution = Double.valueOf(textSimulationResolution.getText());
        } catch (java.lang.NumberFormatException e) {
            mChangedSession.resolution = 0.0;
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
    
    @UiHandler("textMovementRwpWaittime")
    void onMovementRwpWaittimeChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("waittime", textMovementRwpWaittime.getText());
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
    
    @UiHandler("textMovementRwpCoordLat")
    void onMovementRwpCoordLatChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("lat", textMovementRwpCoordLat.getText());
    }
    
    @UiHandler("textMovementRwpCoordLng")
    void onMovementRwpCoordLngChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("lng", textMovementRwpCoordLng.getText());
    }
    
    // RW
    @UiHandler("textMovementRwDuration")
    void onMovementRwDurationChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("duration", textMovementRwDuration.getText());
    }
    
    @UiHandler("textMovementRwAreaSizeHeight")
    void onMovementRwAreaSizeHeightChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("height", textMovementRwAreaSizeHeight.getText());
    }
    
    @UiHandler("textMovementRwAreaSizeWidth")
    void onMovementRwAreaSizeWidthChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("width", textMovementRwAreaSizeWidth.getText());
    }
    
    @UiHandler("textMovementRwMovetime")
    void onMovementRwMovetimeChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("movetime", textMovementRwMovetime.getText());
    }
    
    @UiHandler("textMovementRwVmin")
    void onMovementRwVminChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("vmin", textMovementRwVmin.getText());
    }
    
    @UiHandler("textMovementRwVmax")
    void onMovementRwVmaxChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("vmax", textMovementRwVmax.getText());
    }
    
    @UiHandler("textMovementRwCoordLat")
    void onMovementRwCoordLatChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("lat", textMovementRwCoordLat.getText());
    }
    
    @UiHandler("textMovementRwCoordLng")
    void onMovementRwCoordLngChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("lng", textMovementRwCoordLng.getText());
    }
    
    @UiHandler("textSimulationRange")
    void onSimulationRangeChanged(ChangeEvent evt) {
        try {
            mChangedSession.range = Double.valueOf(textSimulationRange.getText());
        } catch (java.lang.NumberFormatException e) {
            mChangedSession.range = 0.0;
        }
    }
    
    // TRACE
    @UiHandler("buttonMovementTrace")
    void onMovementTraceButtonClicked(ClickEvent e) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("tracefile", "");
        
        buttonMovementTrace.setEnabled(false);
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.removeSessionFile(mSession, "data", listMovementTrace.getValue(), mCredentials, new AsyncCallback<Void>() {
            
            @Override
            public void onSuccess(Void result) {
                refreshDataFiles(mSession);
            }
            
            @Override
            public void onFailure(Throwable caught) {
            }
        });
    }
    
    @UiHandler("formMovementTrace")
    void onMovementTraceSubmit(FormPanel.SubmitEvent event) {
        // TODO: show progress
        formMovementTrace.setVisible(false);
    }
    
    @UiHandler("formMovementTrace")
    void onMovementTraceUploadCompleted(FormPanel.SubmitCompleteEvent event) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("tracefile", uploadMovementTrace.getText());
        
        uploadMovementTrace.setText(null);
        formMovementTrace.setVisible(true);
        
        // refresh file listing
        refreshDataFiles(mSession);
    }
    
    @UiHandler("uploadMovementTrace")
    void onMovementTraceChanged(ChangeEvent evt) {
        formMovementTrace.setEncoding(FormPanel.ENCODING_MULTIPART);
        formMovementTrace.setMethod(FormPanel.METHOD_POST);
        formMovementTrace.setAction(GWT.getModuleBaseURL() + "upload?sid=" + mSession.id);
        formMovementTrace.submit();
    }
    
    @UiHandler("listMovementTrace")
    void onMovementTraceFileChanged(ChangeEvent evt) {
        buttonMovementTrace.setEnabled(!listMovementTrace.getValue().isEmpty());
        
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("tracefile", listMovementTrace.getValue());
    }
    
    @UiHandler("checkMovementTraceRepetition")
    void onMovementTraceRepetitionChanged(ClickEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("repeat", checkMovementTraceRepetition.getValue() ? "yes" : "no");
    }
    
    // STATIC
    @UiHandler("textMovementStaticPositions")
    void onMovementStaticPositionsChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("positions", textMovementStaticPositions.getText());
    }
    
    @UiHandler("textMovementStaticDuration")
    void onMovementStaticDurationChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("duration", textMovementStaticDuration.getText());
    }
    
    @UiHandler("textMovementStaticCoordLat")
    void onMovementStaticoordLatChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("lat", textMovementStaticCoordLat.getText());
    }
    
    @UiHandler("textMovementStaticCoordLng")
    void onMovementStaticCoordLngChanged(ChangeEvent evt) {
        if (mChangedSession.mobility == null) return;
        mChangedSession.mobility.parameters.put("lng", textMovementStaticCoordLng.getText());
    }
    
    @UiHandler("panelTabs")
    void onTabChange(ShownEvent event) {
        if (tabNodes.isActive()) {
            faNodes.setVisible(false);
        } else {
            faNodes.setVisible(true);
        }
    }
}
