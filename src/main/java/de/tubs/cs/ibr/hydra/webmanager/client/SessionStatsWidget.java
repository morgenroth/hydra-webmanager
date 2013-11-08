package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.DataView;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.corechart.AxisOptions;
import com.google.gwt.visualization.client.visualizations.corechart.ColumnChart;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;

import de.tubs.cs.ibr.hydra.webmanager.client.stats.StatsJso;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionStatsWidget extends Composite implements ResizeHandler {

    private static SessionStatsViewUiBinder uiBinder = GWT.create(SessionStatsViewUiBinder.class);
    
    @UiField SimplePanel panelChart;
    
    @UiField Container containerNodeStats;
    
    @UiField ListBox listNodes;
    
    @UiField NavLink linkIpTraffic;
    @UiField NavLink linkDtnTraffic;
    @UiField NavLink linkClockOffset;
    @UiField NavLink linkClockRating;
    @UiField NavLink linkUptime;
    @UiField NavLink linkStorageSize;
    
    // chart objects
    ColumnChart mChart = null;

    // chart data
    DataTable mData = null;
    
    // chart views
    DataView[] mView = { null, null, null, null, null, null };
    
    // currently selected chart
    int mCurrentView = 0;
    
    // list of visible node stats
    HashSet<Long> mVisibleNodes = new HashSet<Long>();
    
    // is true if all charts are initialized
    boolean initialized = false;

    // chart options
    Options[] mOptions = { null, null, null, null, null, null };
    
    private ArrayList<SessionNodeStatsWidget> mNodeStats = new ArrayList<SessionNodeStatsWidget>();
    
    private HashMap<Long, Node> mNodes = new HashMap<Long, Node>();
    private Session mSession = null;

    interface SessionStatsViewUiBinder extends UiBinder<Widget, SessionStatsWidget> {
    }

    public SessionStatsWidget() {
        initWidget(uiBinder.createAndBindUi(this));
        
        // initialize chart options
        updateChartOptions();
        
        // add resize handler
        Window.addResizeHandler(this);
    }
    
    @Override
    public void onResize(ResizeEvent event) {
        updateChartOptions();
        
        // redraw local charts
        redraw(mCurrentView);
        
        // reload stats of nodes
        for (SessionNodeStatsWidget w : mNodeStats) {
            w.onResize(event);
        }
    }
    
    public void onSessionUpdated(Session s) {
        mSession = s;
        
        // push session to sub widgets
        for (SessionNodeStatsWidget w : mNodeStats) {
            w.onSessionUpdated(mSession);
        }
    }
    
    private void redraw(int i) {
        // do not redraw until the chart library has been initialized
        if (!initialized) return;

        // redraw charts
        mChart.draw(mView[i], mOptions[i]);
    }
    
    public void initialize(final Session session) {
        // store session globally
        mSession = session;
        
        // load chart library
        initializeChart();
        
        // load the list of nodes
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getNodes(session.id, new AsyncCallback<ArrayList<Node>>() {
            
            @Override
            public void onSuccess(ArrayList<Node> result) {
                // clear the nodes list box
                listNodes.clear();
                listNodes.addItem("- select a node to add -", "-");
                
                // store the list of nodes
                for (Node n : result) {
                    mNodes.put(n.id, n);
                }
                
                rebuildNodeList();
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed to load node list
            }
        });
    }
    
    private void rebuildNodeList() {
        // clear the nodes list box
        listNodes.clear();
        listNodes.addItem("- select a node to add -", "-");

        LinkedList<Node> nodes = new LinkedList<Node>(mNodes.values());
        Collections.sort(nodes);
        
        // store the list of nodes
        for (Node n : nodes) {
            if (!mVisibleNodes.contains(n.id)) {
                listNodes.addItem(n.name, n.id.toString());
            }
        }
        
        listNodes.setSelectedIndex(0);
    }

    private void initializeChart() {
        Runnable onLoadCallbackColumn = new Runnable() {
            @Override
            public void run() {
                mData = DataTable.create();
                mData.addColumn(ColumnType.STRING, "Nodes");
                mData.addColumn(ColumnType.NUMBER, "TCP (in)");
                mData.addColumn(ColumnType.NUMBER, "TCP (out)");
                mData.addColumn(ColumnType.NUMBER, "UDP (in)");
                mData.addColumn(ColumnType.NUMBER, "UDP (out)");
                mData.addColumn(ColumnType.NUMBER, "Received");
                mData.addColumn(ColumnType.NUMBER, "Transmitted");
                mData.addColumn(ColumnType.NUMBER, "Generated");
                mData.addColumn(ColumnType.NUMBER, "Uptime");
                mData.addColumn(ColumnType.NUMBER, "Storage size");
                mData.addColumn(ColumnType.NUMBER, "Clock Offset");
                mData.addColumn(ColumnType.NUMBER, "DTN Clock Offset");
                mData.addColumn(ColumnType.NUMBER, "DTN Clock Rating");
                
                // create different views
                for (int i = 0; i < mOptions.length; i++) {
                    mView[i] = DataView.create(mData);
                }
                
                mView[0].setColumns(new int[] { 0, 1, 2, 3, 4 });
                mView[1].setColumns(new int[] { 0, 5, 6, 7 });
                mView[2].setColumns(new int[] { 0, 10, 11 });
                mView[3].setColumns(new int[] { 0, 12 });
                mView[4].setColumns(new int[] { 0, 8 });
                mView[5].setColumns(new int[] { 0, 9 });
                
                // create default chart
                mChart = new ColumnChart(mView[0], mOptions[0]);
                panelChart.add(mChart);
                
                // set charts to initialized
                initialized = true;
                
                updateStatsData();
            }
        };
        
        // Load the visualization api, passing the onLoadCallback to be called
        // when loading is done.
        VisualizationUtils.loadVisualizationApi(onLoadCallbackColumn, ColumnChart.PACKAGE);
    }
    
    public void updateStatsData() {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getStatsLatest(mSession, new AsyncCallback<HashMap<Long,DataPoint>>() {
            
            @Override
            public void onSuccess(HashMap<Long, DataPoint> result) {
                transformStatsData(result);
                redraw(mCurrentView);
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed
            }
        });
    }
    
    public void refresh() {
        // reload stats data and redraw charts
        if (initialized) updateStatsData();
        
        // reload stats of nodes
        for (SessionNodeStatsWidget w : mNodeStats) {
            w.refresh();
        }
    }
    
    private void updateChartOptions() {
        // create chart options
        for (int i = 0; i < mOptions.length; i++) {
            if (mOptions[i] == null) {
                mOptions[i] = LineChart.createOptions();
                
                AxisOptions hAxisOptions = AxisOptions.create();
                hAxisOptions.set("slantedText", "true");
                
                mOptions[i].setHAxisOptions(hAxisOptions);
                mOptions[i].setLegend(LegendPosition.BOTTOM);
                
                AxisOptions vAxisOptions = AxisOptions.create();
                
                switch (i) {
                    case 0:
                        mOptions[i].setTitle("IP Traffic");
                        vAxisOptions.setTitle("bytes");
                        break;
                    case 1:
                        mOptions[i].setTitle("DTN Traffic");
                        vAxisOptions.setTitle("bundles");
                        break;
                    case 2:
                        mOptions[i].setTitle("Clock Offset");
                        vAxisOptions.setTitle("seconds");
                        break;
                    case 3:
                        mOptions[i].setTitle("Clock Rating");
                        break;
                    case 4:
                        mOptions[i].setTitle("Uptime");
                        vAxisOptions.setTitle("seconds");
                        break;
                    case 5:
                        mOptions[i].setTitle("Storage size");
                        vAxisOptions.setTitle("bytes");
                        break;
                    default:
                        // no title
                        break;
                }
                
                mOptions[i].setVAxisOptions(vAxisOptions);
                //mOptions[i].setChartArea(area);
            }
        }
        
        Double width = Double.valueOf(panelChart.getOffsetWidth());
        Double height = Double.valueOf(width) * Double.valueOf(9.0 / 16.0);
        
        for (int i = 0; i < mOptions.length; i++) {
            if (width > 0) {
                mOptions[i].setWidth(width.intValue());
                mOptions[i].setHeight(height.intValue());
            } else {
                mOptions[i].setWidth(360);
                mOptions[i].setHeight(480);
            }
        }
    }
    
    private void transformStatsData(HashMap<Long, DataPoint> result) {
        if (result == null) return;
        
        // add more rows if necessary
        int nor = mData.getNumberOfRows();
        if (nor < result.size()) {
            mData.addRows(result.size() - nor);
        }
        
        Integer row = 0;
        
        // sort the nodes
        LinkedList<Long> nodeids = new LinkedList<Long>(result.keySet());
        Collections.sort(nodeids);
        
        for (Long nodeId : nodeids) {
            DataPoint data = result.get(nodeId);
            
            // convert json to object
            StatsJso stats = StatsJso.create(data.json);
            
            mData.setValue(row, 0, mNodes.get(nodeId).name);
            mData.setValue(row, 1, stats.getTraffic().getInTcpByte());
            mData.setValue(row, 2, stats.getTraffic().getOutTcpByte());
            mData.setValue(row, 3, stats.getTraffic().getInUdpByte());
            mData.setValue(row, 4, stats.getTraffic().getOutUdpByte());

            mData.setValue(row, 5, stats.getDtnd().getBundles().getReceived());
            mData.setValue(row, 6, stats.getDtnd().getBundles().getTransmitted());
            mData.setValue(row, 7, stats.getDtnd().getBundles().getGenerated());
            
            mData.setValue(row, 8, stats.getDtnd().getInfo().getUptime());
            mData.setValue(row, 9, stats.getDtnd().getInfo().getStorageSize());
            
            mData.setValue(row, 10, stats.getClock().getOffset());
            mData.setValue(row, 11, stats.getDtnd().getTimeSync().getOffset());
            mData.setValue(row, 12, stats.getDtnd().getTimeSync().getRating());
            
            row++;
        }
    }
    
    @UiHandler("listNodes")
    void onNodeSelected(ChangeEvent e) {
        // create individual node stats
        int index = listNodes.getSelectedIndex();
        String value = listNodes.getValue(index);
        listNodes.removeItem(index);
        listNodes.setSelectedIndex(0);
        
        Node n = mNodes.get(Long.valueOf(value));
        
        if (n != null) {
            mVisibleNodes.add(n.id);
            
            SessionNodeStatsWidget ns = new SessionNodeStatsWidget(mRemoveListener, mSession, n);
            mNodeStats.add(ns);
            containerNodeStats.add(ns);
        }
    }
    
    private SessionNodeStatsWidget.OnStatsRemovedListener mRemoveListener = new SessionNodeStatsWidget.OnStatsRemovedListener() {
        @Override
        public void onStatsRemoved(Node n) {
            mVisibleNodes.remove(n.id);
            rebuildNodeList();
        }
    };
    
    @UiHandler("linkIpTraffic")
    public void onNavIpTrafficClick(ClickEvent evt) {
        mCurrentView = 0;
        redraw(mCurrentView);
    }
    
    @UiHandler("linkDtnTraffic")
    public void onNavDtnTrafficClick(ClickEvent evt) {
        mCurrentView = 1;
        redraw(mCurrentView);
    }
    
    @UiHandler("linkClockOffset")
    public void onNavClockOffsetClick(ClickEvent evt) {
        mCurrentView = 2;
        redraw(mCurrentView);
    }
    
    @UiHandler("linkClockRating")
    public void onNavClockRatingClick(ClickEvent evt) {
        mCurrentView = 3;
        redraw(mCurrentView);
    }
    
    @UiHandler("linkUptime")
    public void onNavUptimeClick(ClickEvent evt) {
        mCurrentView = 4;
        redraw(mCurrentView);
    }
    
    @UiHandler("linkStorageSize")
    public void onNavStorageSizeClick(ClickEvent evt) {
        mCurrentView = 5;
        redraw(mCurrentView);
    }
}
