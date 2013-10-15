package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.Selection;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.visualizations.ColumnChart;
import com.google.gwt.visualization.client.visualizations.LineChart;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventExtra;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SessionWatchView extends View {

    private static SessionWatchViewUiBinder uiBinder = GWT.create(SessionWatchViewUiBinder.class);
    
    @UiField Button buttonBack;
    @UiField CellTable<Node> tableNodes;
    
    @UiField TextBox textDetailsState;
    @UiField ProgressBar progressDetails;
    @UiField TextBox textDetailsElapsedTime;
    @UiField TextBox textDetailsDesc;
    
    @UiField SimplePanel panelStatsTraffic;
    @UiField SimplePanel panelStatsBundles;
    @UiField SimplePanel panelStatsTrafficNode;
    @UiField SimplePanel panelStatsBundlesNode;
    @UiField SimplePanel panelStatsClockNode;
    
    @UiField Heading headingStatsTrafficNode;
    @UiField Heading headingStatsBundlesNode;
    @UiField Heading headingStatsClockNode;
    
    interface Style extends CssResource {
        String activated();
        String enabled();
        String disabled();
        String error();
    }
    
    @UiField Style style;
    
    // data provider for the node table
    ListDataProvider<Node> mDataProvider = new ListDataProvider<Node>();
    
    // chart objects
    ColumnChart mChartTraffic = null;
    ColumnChart mChartBundles = null;
    LineChart mChartTrafficNode = null;
    LineChart mChartBundlesNode = null;
    LineChart mChartClockNode = null;
    
    // chart data
    DataTable mDataChartTraffic = null;
    DataTable mDataChartBundles = null;
    DataTable mDataChartTrafficNode = null;
    DataTable mDataChartBundlesNode = null;
    DataTable mDataChartClockNode = null;
    
    // chart options
    ColumnChart.Options mOptionsChart = null;
    LineChart.Options mOptionsChartNode = null;
    
    // selection stats node
    Node mSelectedNode = null;
    
    // currently viewed session
    Session mSession = null;
    
    // list of slaves
    ArrayList<Slave> mSlaves = new ArrayList<Slave>();

    interface SessionWatchViewUiBinder extends UiBinder<Widget, SessionWatchView> {
    }

    public SessionWatchView(HydraApp app, Session s) {
        super(app);
        initWidget(uiBinder.createAndBindUi(this));
        
        // create node table
        createNodeTable();

        // initialize the session
        refreshSlaves();
        refreshSession(s);
        
        // initialize statistic visualization
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getNodes(s.id, new AsyncCallback<ArrayList<Node>>() {
            
            @Override
            public void onSuccess(ArrayList<Node> result) {
                if (!result.isEmpty()) {
                    initializeStats(result.get(0));
                }
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed to load node list
            }
        });
    }
    
    private void initializeStats(final Node defaultNode) {
        Runnable onLoadCallbackColumn = new Runnable() {
            @Override
            public void run() {
                mOptionsChart = ColumnChart.Options.create();
                mOptionsChart.setLegend(LegendPosition.BOTTOM);
                mOptionsChart.setWidth(500);
                mOptionsChart.setHeight(320);
                
                updateStats(new Runnable() {
                    @Override
                    public void run() {
                        mChartTraffic = new ColumnChart(mDataChartTraffic, mOptionsChart);
                        mChartTraffic.addSelectHandler(createSelectHandler(mChartTraffic));
                        panelStatsTraffic.add(mChartTraffic);
                        
                        mChartBundles = new ColumnChart(mDataChartBundles, mOptionsChart);
                        mChartBundles.addSelectHandler(createSelectHandler(mChartBundles));
                        panelStatsBundles.add(mChartBundles);
                    }
                }, true);
            }
        };
        
        Runnable onLoadCallbackLine = new Runnable() {
            @Override
            public void run() {
                mOptionsChartNode = LineChart.Options.create();
                mOptionsChartNode.setLegend(LegendPosition.BOTTOM);
                mOptionsChartNode.setWidth(360);
                mOptionsChartNode.setHeight(320);
                
                updateStats(defaultNode, new Runnable() {
                    @Override
                    public void run() {
                        mChartTrafficNode = new LineChart(mDataChartTrafficNode, mOptionsChartNode);
                        panelStatsTrafficNode.add(mChartTrafficNode);
                        
                        mChartBundlesNode = new LineChart(mDataChartBundlesNode, mOptionsChartNode);
                        panelStatsBundlesNode.add(mChartBundlesNode);
                        
                        mChartClockNode = new LineChart(mDataChartClockNode, mOptionsChartNode);
                        panelStatsClockNode.add(mChartClockNode);
                    }
                }, true);
            }
        };
        
        // Load the visualization api, passing the onLoadCallback to be called
        // when loading is done.
        VisualizationUtils.loadVisualizationApi(onLoadCallbackColumn, ColumnChart.PACKAGE);
        
        // Load the visualization api, passing the onLoadCallback to be called
        // when loading is done.
        VisualizationUtils.loadVisualizationApi(onLoadCallbackLine, LineChart.PACKAGE);
    }
    
    public void updateStats(final Runnable callback, final boolean rebuild) {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getStatsLatest(mSession, new AsyncCallback<HashMap<Long,String>>() {
            
            @Override
            public void onSuccess(HashMap<Long, String> result) {
                refreshStats(result, rebuild);
                callback.run();
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed
            }
        });
    }
    
    public void updateStats(final Node n, final Runnable callback, final boolean rebuild) {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        
        mcs.getStatsData(mSession, n, mSession.started, null, new AsyncCallback<HashMap<Long,HashMap<Long,String>>>() {
            
            @Override
            public void onSuccess(HashMap<Long,HashMap<Long,String>> result) {
                refreshStats(n, result, rebuild);
                
                // store selected node locally
                mSelectedNode = n;
                
                callback.run();
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed
            }
        });
    }
    
    private void refreshStats() {
        updateStats(new Runnable() {
            @Override
            public void run() {
                mChartTraffic.draw(mDataChartTraffic, mOptionsChart);
                mChartBundles.draw(mDataChartBundles, mOptionsChart);
            }
        }, false);
        
        if (mSelectedNode != null) {
            updateStats(mSelectedNode, new Runnable() {
                @Override
                public void run() {
                    mChartTrafficNode.draw(mDataChartTrafficNode, mOptionsChartNode);
                    mChartBundlesNode.draw(mDataChartBundlesNode, mOptionsChartNode);
                    mChartClockNode.draw(mDataChartClockNode, mOptionsChartNode);
                }
            }, false);
        }
    }
    
    private void refreshStats(Node n, HashMap<Long, HashMap<Long,String>> result, boolean rebuild) {
        if (rebuild) {
            mDataChartTrafficNode = DataTable.create();
            mDataChartTrafficNode.addColumn(ColumnType.NUMBER, "Traffic");
            
            mDataChartBundlesNode = DataTable.create();
            mDataChartBundlesNode.addColumn(ColumnType.NUMBER, "Received");
            mDataChartBundlesNode.addColumn(ColumnType.NUMBER, "Transmitted");
            mDataChartBundlesNode.addColumn(ColumnType.NUMBER, "Generated");
            
            mDataChartClockNode = DataTable.create();
            mDataChartClockNode.addColumn(ColumnType.NUMBER, "Offset");
        }
        
        // iterate through all the data
        for (Entry<Long,HashMap<Long,String>> e : result.entrySet()) {
            Long timestamp = e.getKey();
            HashMap<Long,String> nodes_data = e.getValue();
            
            // get node data
            JSONObject obj = JSONParser.parseStrict(nodes_data.get(n.id)).isObject();
            
            if (obj != null) {
                System.out.println("JSON node data @ " + timestamp + ": " + obj);
            }
        }
    }
    
    private void refreshStats(HashMap<Long, String> result, boolean rebuild) {
        if (rebuild) {
            mDataChartBundles = DataTable.create();
            mDataChartBundles.addColumn(ColumnType.STRING, "Nodes");
            mDataChartBundles.addColumn(ColumnType.NUMBER, "Received");
            mDataChartBundles.addColumn(ColumnType.NUMBER, "Transmitted");
            mDataChartBundles.addColumn(ColumnType.NUMBER, "Generated");
            mDataChartBundles.addRows(result.size());
            
            mDataChartTraffic = null;
        }

        Integer row = 0;
        for (Entry<Long,String> e : result.entrySet()) {
            Long nodeId = e.getKey();
            String data = e.getValue();
            
            JSONObject obj = JSONParser.parseStrict(data).isObject();
            
            if (obj != null) {
                //System.out.println("JSON data: " + obj);
                
                if (obj.containsKey("iface")) {
                    // get json object for 'iface'
                    JSONObject iface_data = obj.get("iface").isObject();
                    
                    // generate the traffic columns
                    if (mDataChartTraffic == null) {
                        mDataChartTraffic = DataTable.create();
                        
                        mDataChartTraffic.addColumn(ColumnType.STRING, "Nodes");
                        
                        for (String key : iface_data.keySet()) {
                            // skip "lo" and "eth1" interface
                            if (key.equals("lo") || key.equals("eth1")) continue;
                            
                            mDataChartTraffic.addColumn(ColumnType.NUMBER, key + " (rx)");
                            mDataChartTraffic.addColumn(ColumnType.NUMBER, key + " (tx)");
                        }
                        
                        mDataChartTraffic.addRows(result.size());
                    }
                    
                    mDataChartTraffic.setValue(row, 0, nodeId.toString());
                    Integer index = 1;
                    
                    for (String key : iface_data.keySet()) {
                        // skip "lo" and "eth1" interface
                        if (key.equals("lo") || key.equals("eth1")) continue;
                        
                        JSONObject if_data = iface_data.get(key).isObject();
                        
                        Long rx = Long.valueOf(if_data.get("rx").isString().stringValue());
                        Long tx = Long.valueOf(if_data.get("tx").isString().stringValue());
                        
                        mDataChartTraffic.setValue(row, index, rx);
                        mDataChartTraffic.setValue(row, index + 1, tx);
                        
                        index += 2;
                    }
                }
                
                if (obj.containsKey("dtnd")) {
                    // get json object for 'dtnd'
                    JSONObject dtn_data = obj.get("dtnd").isObject();
                    
                    if (dtn_data.containsKey("bundles")) {
                        // get json object for 'bundles'
                        JSONObject bundle_data = dtn_data.get("bundles").isObject();
                        
                        Long received = Long.valueOf(bundle_data.get("Received").isString().stringValue());
                        Long transmitted = Long.valueOf(bundle_data.get("Transmitted").isString().stringValue());
                        Long generated = Long.valueOf(bundle_data.get("Generated").isString().stringValue());
                        
                        mDataChartBundles.setValue(row, 0, nodeId.toString());
                        mDataChartBundles.setValue(row, 1, received);
                        mDataChartBundles.setValue(row, 2, transmitted);
                        mDataChartBundles.setValue(row, 3, generated);
                    }
                }
                
                row++;
            }
        }
    }
    
    private void refresh() {
        if (mSession == null) return;
        
        // load session properties
        refreshSession(mSession);
    }
    
    private void update(Session s) {
        // store data locally
        mSession = s;

        // update session state
        textDetailsState.setText(s.state.toString());
        textDetailsDesc.setText(s.name);
        
        // update the progress bar and clock
        updateProgress(s);
        
        // load nodes
        refreshNodeTable(s);
        
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
            case RANDOM_WALK:
                if (s.mobility.parameters.containsKey("duration")) {
                    duration = Long.valueOf(s.mobility.parameters.get("duration"));
                }
                break;
            case STATIC:
                break;
            case THE_ONE:
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
        
        if (Session.State.RUNNING.equals(s.state)) {
            // animate the progress bar in RUNNING state
            progressDetails.setType(ProgressBar.Style.ANIMATED);
        } else if (Session.State.PENDING.equals(s.state)) {
            // animate the progress bar in PENDING state
            progressDetails.setType(ProgressBar.Style.ANIMATED);
        } else if (Session.State.DRAFT.equals(s.state)) {
            // set progress bar to zero in DRAFT state
            progressDetails.setPercent(0);
        } else {
            // do not animate the progress bar in other states
            progressDetails.setType(ProgressBar.Style.STRIPED);
        }
        
        if (duration == null) {
            progressDetails.setPercent(100);
            if (elapsedSeconds == null) {
                textDetailsElapsedTime.setText(getDurationString(0));
            } else {
                textDetailsElapsedTime.setText(getDurationString(elapsedSeconds));
            }
        } else {
            if (elapsedSeconds == null) {
                progressDetails.setPercent(100);
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
    public void eventRaised(Event evt) {
        // refresh table on refresh event
        if (EventType.NODE_STATE_CHANGED.equals(evt)) {
            // do not update, if we don't have a session
            if (mSession == null) return;
            
            if (isRelated(evt)) {
                // refresh nodes
                refreshNodeTable(mSession);
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
            refreshSlaves();
        }
        else if (EventType.SESSION_STATS_UPDATED.equals(evt)) {
            if (isRelated(evt)) {
                refreshStats();
            }
        }
    }
    
    private boolean isRelated(Event evt) {
        if (evt.getExtras() == null) return true;
        if (mSession == null) return false;
        
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
    void onClick(ClickEvent e) {
        // switch back to session view
        resetView();
    }
    
    /*** CHART DEMO CODE ***/
      private SelectHandler createSelectHandler(final ColumnChart chart) {
        return new SelectHandler() {
          @Override
          public void onSelect(SelectEvent event) {
            String message = "";
            
            // May be multiple selections.
            JsArray<Selection> selections = chart.getSelections();

            for (int i = 0; i < selections.length(); i++) {
              // add a new line for each selection
              message += i == 0 ? "" : "\n";
              
              Selection selection = selections.get(i);

              if (selection.isCell()) {
                // isCell() returns true if a cell has been selected.
                
                // getRow() returns the row number of the selected cell.
                int row = selection.getRow();
                // getColumn() returns the column number of the selected cell.
                int column = selection.getColumn();
                message += "cell " + row + ":" + column + " selected";
              } else if (selection.isRow()) {
                // isRow() returns true if an entire row has been selected.
                
                // getRow() returns the row number of the selected row.
                int row = selection.getRow();
                message += "row " + row + " selected";
              } else {
                // unreachable
                message += "Pie chart selections should be either row selections or cell selections.";
                message += "  Other visualizations support column selections as well.";
              }
            }
            
            Window.alert(message);
          }
        };
      }
}
