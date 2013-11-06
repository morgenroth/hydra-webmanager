package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
import com.google.gwt.visualization.client.DataView;
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
    DataTable mData = null;
    
    // chart views
    DataView mViewTraffic = null;
    DataView mViewBundles = null;
    
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
        mChartTraffic.draw(mViewTraffic, mOptionsChart);
    
        // redraw bundles chart
        mChartBundles.draw(mViewBundles, mOptionsChart);
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
                
                mChartTraffic = new ColumnChart();
                panelTraffic.add(mChartTraffic);
                
                mChartBundles = new ColumnChart();
                panelBundles.add(mChartBundles);
                
                // create different views
                mViewTraffic = DataView.create(mData);
                mViewTraffic.setColumns(new int[] { 0, 1, 2 });
                
                mViewBundles = DataView.create(mData);
                mViewBundles.setColumns(new int[] { 0, 5, 6, 7 });
                
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
        int nor = mData.getNumberOfRows();
        if (nor < result.size()) {
            mData.addRows(result.size() - nor);
        }
        
        Integer row = 0;
        
        for (Entry<Long,DataPoint> e : result.entrySet()) {
            Long nodeId = e.getKey();
            DataPoint data = e.getValue();
            
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
