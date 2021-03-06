package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel.ShownEvent;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase.Color;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SessionWatchView extends View {

    private static SessionWatchViewUiBinder uiBinder = GWT.create(SessionWatchViewUiBinder.class);
    
    @UiField CellTable<Node> tableNodes;
    
    @UiField TextBox textDetailsState;
    @UiField ProgressBar progressDetails;
    @UiField TextBox textDetailsElapsedTime;
    @UiField TextBox textDetailsDesc;
    
    @UiField SessionStatsWidget statsView;
    @UiField SessionMapWidget mapView;
    @UiField SessionDownloadWidget downloadView;
    
    @UiField Tab tabMapView;
    
    interface Style extends CssResource {
        String activated();
        String enabled();
        String disabled();
        String error();
    }
    
    @UiField Style style;
    
    // data provider for the node table
    ListDataProvider<Node> mDataProvider = new ListDataProvider<Node>();
    
    // currently viewed session
    Session mSession = null;
    
    // initialized flag
    boolean mInitialized = false;
    
    // list of slaves
    ArrayList<Slave> mSlaves = new ArrayList<Slave>();

    interface SessionWatchViewUiBinder extends UiBinder<Widget, SessionWatchView> {
    }

    public SessionWatchView(HydraApp app, Session s) {
        super(app);
        initWidget(uiBinder.createAndBindUi(this));
        
        // assign current session
        mSession = s;
        
        // create node table
        createNodeTable();

        // initialize the session
        refreshSlaves();
        refreshSession(s);
        
        // initialize stats view
        statsView.initialize(s);
        
        // initialize map view
        mapView.initialize(s);
        
        // initialize download view
        downloadView.initialize(s);
    }
    
    @Override
    protected void onAttach() {
        super.onAttach();
        
        // subscribe to session details
        getApplication().subscribeAtmosphere(mSession.id);
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        
        // un-subscribe to session details
        getApplication().unsubscribeAtmosphere(mSession.id);
    }

    private void refresh() {
        if (!mInitialized) return;
        
        // load session properties
        refreshSession(mSession);
    }
    
    private void update(Session s) {
        // store data locally
        mSession = s;

        // update session state
        textDetailsState.setText(s.state.toString());
        textDetailsDesc.setText(s.name);
        
        // mark as initialized
        mInitialized = true;
        
        // update the progress bar and clock
        updateProgress(s);
        
        // load nodes
        refreshNodeTable(s);
        
        // push updated session to stats widgets
        statsView.onSessionUpdated(mSession);
        
        // push updated session to map widget
        mapView.onSessionUpdated(mSession);
        
        // push updated session to download widget
        downloadView.onSessionUpdated(mSession);
        
        // schedule / cancel progress update timer
        if (Session.State.RUNNING.equals(s.state)) {
            mRefreshProgressTimer.scheduleRepeating(100);
        } else {
            mRefreshProgressTimer.cancel();
        }
    }
    
    private void updateProgress(Session s) {
        Long duration = null;
        
        switch (s.mobility.model) {
            case RANDOM_WAYPOINT:
                if (s.mobility.parameters.containsKey("duration")) {
                    duration = Long.valueOf(s.mobility.parameters.get("duration"));
                }
                break;
            case RANDOM_WALK:
                if (s.mobility.parameters.containsKey("duration")) {
                    duration = Long.valueOf(s.mobility.parameters.get("duration"));
                }
                break;
            case STATIC:
                if (s.mobility.parameters.containsKey("duration")) {
                    duration = Long.valueOf(s.mobility.parameters.get("duration"));
                }
                break;
            case TRACE:
                break;
            default:
                duration = null;
                break;
        }
        
        Long elapsedSeconds = null;
        
        if (s.started != null) {
            Date now = null;
            
            if (s.finished != null) {
                now = s.finished;
            }
            else if (s.aborted != null) {
                now = s.aborted;
            }
            else {
                now = new Date();
            }
            
            // calculate the number of seconds since the progress has been started
            elapsedSeconds = (now.getTime() - s.started.getTime()) / 1000;
        }
        
        // set progress color according to the session state
        switch (s.state) {
            case ERROR:
                // animate the progress bar in PENDING state
                progressDetails.setType(ProgressBar.Style.STRIPED);
                progressDetails.setColor(Color.DANGER);
                break;
            case FINISHED:
                // set progress bar to full in FINISHED state
                progressDetails.setPercent(100);
                
                // animate the progress bar in PENDING state
                progressDetails.setType(ProgressBar.Style.STRIPED);
                progressDetails.setColor(Color.SUCCESS);
                break;
            case PENDING:
                // animate the progress bar in PENDING state
                progressDetails.setType(ProgressBar.Style.ANIMATED);
                progressDetails.setColor(Color.WARNING);
                
                // update pending progress
                updatePendingProgress();
                break;
            case RUNNING:
                // animate the progress bar in RUNNING state
                progressDetails.setType(ProgressBar.Style.ANIMATED);
                progressDetails.setColor(Color.SUCCESS);
                
                // set progress to 100% if no duration is known
                if (duration == null) progressDetails.setPercent(100);
                break;
            default:
                // set progress bar to zero in DRAFT state
                progressDetails.setPercent(0);
                
                // do not animate the progress bar in other states
                progressDetails.setType(ProgressBar.Style.STRIPED);
                progressDetails.setColor(Color.DEFAULT);
                break;
        }
        
        // update elapsed seconds
        updateElapsedSeconds(duration, elapsedSeconds);
    }
    
    private void updatePendingProgress() {
        // get list of all nodes
        List<Node> nodes = mDataProvider.getList();
        
        if (nodes.size() <= 0) {
            progressDetails.setPercent(0);
            return;
        }
        
        // create counter for the different node states
        int[] state = { 0, 0, 0, 0 };
        
        for (Node n : nodes) {
            switch (n.state) {
                case CONNECTED:
                    state[3]++;
                    break;
                case CREATED:
                    state[2]++;
                    break;
                case SCHEDULED:
                    state[1]++;
                    break;
                default:
                    state[0]++;
                    break;
            }
        }
        
        int progress_points = (state[1] + (state[2] * 2) + (state[3] * 3));
        int percent = progress_points * 100 / (3 * nodes.size());
        progressDetails.setPercent(percent);
    }
    
    private void updateElapsedSeconds(Long duration, Long elapsedSeconds) {
        if (duration == null) {
            if (elapsedSeconds == null) {
                textDetailsElapsedTime.setText(getDurationString(0));
            } else {
                textDetailsElapsedTime.setText(getDurationString(elapsedSeconds));
            }
        } else {
            if (elapsedSeconds == null) {
                textDetailsElapsedTime.setText(getDurationString(0) + " / " + getDurationString(duration));
            } else {
                progressDetails.setPercent(Long.valueOf((elapsedSeconds * 100) / duration).intValue());
                textDetailsElapsedTime.setText(getDurationString(elapsedSeconds) + " / " + getDurationString(duration));
            }
        }
    }
    
    private Timer mRefreshProgressTimer = new Timer() {
        @Override
        public void run() {
            updateProgress(mSession);
        }
    };
    
    private String getDurationString(long duration) {
        long hours = duration / 3600;
        long minutes = (duration % 3600) / 60;
        long seconds = duration % 60;
        
        NumberFormat f = NumberFormat.getFormat("#00");
        return f.format(hours) + ":" + f.format(minutes) + ":" + f.format(seconds);
    }
    
    private Slave getSlave(Long id) {
        for (Slave s : mSlaves) {
            if (id.equals(s.id)) {
                return s;
            }
        }
        return null;
    }
    
    private void refreshSlaves() {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getSlaves(new AsyncCallback<ArrayList<Slave>>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(ArrayList<Slave> result) {
                mSlaves = result;
                
                // refresh node table
                tableNodes.redraw();
            }

        });
    }
    
    private void refreshSession(Session session) {
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
                update(result);
            }
        });
    }
    
    private void refreshNodeTable(Session s) {
        if (s == null) return;
        
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getNodes(s.id, new AsyncCallback<java.util.ArrayList<de.tubs.cs.ibr.hydra.webmanager.shared.Node>>() {

            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(ArrayList<Node> result) {
                List<Node> list = mDataProvider.getList();
                list.clear();
                for (Node n : result) {
                    list.add(n);
                }
                
                updateProgress(mSession);
            }
            
        });
    }
    
    private void createNodeTable() {
        mDataProvider = new ListDataProvider<Node>();
        mDataProvider.addDataDisplay(tableNodes);
        
        // set table name
        tableNodes.setTitle("Nodes");
        
        // add common headers
        addNodeHeaders(tableNodes);
    }
    
    public void addNodeHeaders(CellTable<Node> table) {
        /**
         * id column
         */
        TextColumn<Node> idColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                return s.id.toString();
            }
        };
        
        table.addColumn(idColumn, "ID");
        table.setColumnWidth(idColumn, 6, Unit.EM);
        
        /**
         * slave column
         */
        TextColumn<Node> slaveColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node n) {
                Long slaveId = n.slaveId;
                
                // show assigned slave, if assigned
                if (n.assignedSlaveId != null)
                    slaveId = n.assignedSlaveId;
                
                if (slaveId == null) {
                    return "<not assigned>";
                }
                Slave sobj = getSlave(slaveId);
                if (sobj == null) {
                    return "<missing>";
                }
                return sobj.name;
            }

            @Override
            public String getCellStyleNames(Context context, Node n) {
                Long slaveId = n.slaveId;
                
                // show assigned slave, if assigned
                if (n.assignedSlaveId != null)
                    slaveId = n.assignedSlaveId;
                
                if (slaveId == null) {
                    return style.disabled();
                }
                Slave sobj = getSlave(slaveId);
                if (sobj == null) {
                    return style.error();
                }
                
                if (Slave.State.DISCONNECTED.equals(sobj.state)) {
                    if (n.assignedSlaveId != null) return style.error();
                    return style.disabled();
                }
                
                if (n.assignedSlaveId != null) return style.activated();
                return style.enabled();
            }
        };
        
        table.addColumn(slaveColumn, "Slave");
        table.setColumnWidth(slaveColumn, 12, Unit.EM);
        
        /**
         * name column
         */
        TextColumn<Node> nameColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                if (s.name == null) return "<unnamed>";
                return s.name;
            }
        };

        table.addColumn(nameColumn, "Name");
        
        /**
         * address column
         */
        TextColumn<Node> addressColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                if (s.address == null) return "<not assigned>";
                return s.address;
            }
            
            @Override
            public String getCellStyleNames(Context context, Node n) {
                if (n.address == null) return style.disabled();
                return style.activated();
            }
        };

        table.addColumn(addressColumn, "Address");
        table.setColumnWidth(addressColumn, 12, Unit.EM);
        
        /**
         * state column
         */
        TextColumn<Node> stateColumn = new TextColumn<Node>() {
            @Override
            public String getValue(Node s) {
                if (s.state == null) return "<unknown>";
                return s.state.toString();
            }
        };
        
        stateColumn.setSortable(true);
        stateColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        table.addColumn(stateColumn, "State");
        table.setColumnWidth(stateColumn, 8, Unit.EM);
    }

    @Override
    public void onEventRaised(Event evt) {
        // refresh table on refresh event
        if (evt.equals(EventType.NODE_STATE_CHANGED)) {
            // do not update, if we don't have a session
            if (!mInitialized) return;
            
            if (isRelated(evt)) {
                // refresh nodes
                refreshNodeTable(mSession);
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
            refreshSlaves();
        }
        else if (evt.equals(EventType.SESSION_STATS_UPDATED)) {
            if (isRelated(evt)) {
                statsView.refresh();
            }
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

    @UiHandler("panelTabs")
    void onTabChange(ShownEvent event) {
        statsView.onResize(null);
        mapView.setVisible(tabMapView.isActive());
    }
}
