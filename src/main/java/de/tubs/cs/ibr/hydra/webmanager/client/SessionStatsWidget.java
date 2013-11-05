package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.github.gwtbootstrap.client.ui.Container;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
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
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.ColumnChart;

import de.tubs.cs.ibr.hydra.webmanager.client.stats.StatsJso;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionStatsWidget extends Composite implements ResizeHandler {

    private static SessionStatsViewUiBinder uiBinder = GWT.create(SessionStatsViewUiBinder.class);
    
    @UiField SimplePanel panelTraffic;
    @UiField SimplePanel panelBundles;
    
    @UiField Container containerNodeStats;
    
    @UiField ListBox listNodes;
    
    // chart objects
    ColumnChart mChartTraffic = null;
    ColumnChart mChartBundles = null;

    // chart data
    DataTable mDataChartTraffic = null;
    DataTable mDataChartBundles = null;
    
    // list of visible node stats
    HashSet<Long> mVisibleNodes = new HashSet<Long>();
    
    // is true if all charts are initialized
    boolean initialized = false;

    // chart options
    ColumnChart.Options mOptionsChart = null;
    
    private ArrayList<SessionNodeStatsWidget> mNodeStats = new ArrayList<SessionNodeStatsWidget>();
    
    private HashMap<Long, Node> mNodes = new HashMap<Long, Node>();
    private Session mSession = null;

    interface SessionStatsViewUiBinder extends UiBinder<Widget, SessionStatsWidget> {
    }

    public SessionStatsWidget() {
        initWidget(uiBinder.createAndBindUi(this));
        
        // create chart options
        mOptionsChart = ColumnChart.Options.create();
        mOptionsChart.setLegend(LegendPosition.BOTTOM);
        mOptionsChart.setWidth(640);
        mOptionsChart.setHeight(480);
        
        // add resize handler
        Window.addResizeHandler(this);
    }
    
    @Override
    public void onResize(ResizeEvent event) {
        Integer width = panelTraffic.getOffsetWidth();
        Double height = Double.valueOf(width) * Double.valueOf(9.0 / 16.0);
        
        if (width > 0) {
            mOptionsChart.setSize(width, height.intValue());
        
            // redraw local charts
            redraw();
        }
        
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
    
    private void redraw() {
        // do not redraw until the chart library has been initialized
        if (!initialized) return;

        // redraw traffic chart
        mChartTraffic.draw(mDataChartTraffic, mOptionsChart);
    
        // redraw bundles chart
        mChartBundles.draw(mDataChartBundles, mOptionsChart);
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
        
        // store the list of nodes
        for (Node n : mNodes.values()) {
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
                mDataChartTraffic = DataTable.create();
                mDataChartTraffic.addColumn(ColumnType.STRING, "Nodes");
                mDataChartTraffic.addColumn(ColumnType.NUMBER, "TCP (in)");
                mDataChartTraffic.addColumn(ColumnType.NUMBER, "TCP (out)");
                //mDataChartTraffic.addColumn(ColumnType.NUMBER, "UDP (in)");
                //mDataChartTraffic.addColumn(ColumnType.NUMBER, "UDP (out)");
                
                mChartTraffic = new ColumnChart(mDataChartTraffic, mOptionsChart);
                panelTraffic.add(mChartTraffic);
                //mChartTraffic.addSelectHandler(createSelectHandler(mChartTraffic));
                
                mDataChartBundles = DataTable.create();
                mDataChartBundles.addColumn(ColumnType.STRING, "Nodes");
                mDataChartBundles.addColumn(ColumnType.NUMBER, "Received");
                mDataChartBundles.addColumn(ColumnType.NUMBER, "Transmitted");
                mDataChartBundles.addColumn(ColumnType.NUMBER, "Generated");
                
                mChartBundles = new ColumnChart(mDataChartBundles, mOptionsChart);
                
                panelBundles.add(mChartBundles);
                //mChartBundles.addSelectHandler(createSelectHandler(mChartBundles));
                
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
                redraw();
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
    
    private void transformStatsData(HashMap<Long, DataPoint> result) {
        // add more rows if necessary
        int nor = mDataChartBundles.getNumberOfRows();
        if (nor < result.size()) {
            mDataChartBundles.addRows(result.size() - nor);
            mDataChartTraffic.addRows(result.size() - nor);
        }
        
        Integer row = 0;
        
        for (Entry<Long,DataPoint> e : result.entrySet()) {
            Long nodeId = e.getKey();
            DataPoint data = e.getValue();
            
            // convert json to object
            StatsJso stats = StatsJso.create(data.json);
            
            mDataChartTraffic.setValue(row, 0, mNodes.get(nodeId).name);
            mDataChartTraffic.setValue(row, 1, stats.getTraffic().getInTcpByte());
            mDataChartTraffic.setValue(row, 2, stats.getTraffic().getOutTcpByte());
            //mDataChartTraffic.setValue(row, 3, stats.getTraffic().getInUdpByte());
            //mDataChartTraffic.setValue(row, 4, stats.getTraffic().getOutUdpByte());
            
            mDataChartBundles.setValue(row, 0, mNodes.get(nodeId).name);
            mDataChartBundles.setValue(row, 1, stats.getDtnd().getBundles().getReceived());
            mDataChartBundles.setValue(row, 2, stats.getDtnd().getBundles().getTransmitted());
            mDataChartBundles.setValue(row, 3, stats.getDtnd().getBundles().getGenerated());
            
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
}
