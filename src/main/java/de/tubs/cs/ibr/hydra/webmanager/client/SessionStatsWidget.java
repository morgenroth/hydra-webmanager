package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.github.gwtbootstrap.client.ui.Container;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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

import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionStatsWidget extends Composite implements ResizeHandler {

    private static SessionStatsViewUiBinder uiBinder = GWT.create(SessionStatsViewUiBinder.class);
    
    @UiField SimplePanel panelTraffic;
    @UiField SimplePanel panelBundles;
    
    @UiField Container containerNodeStats;
    
    // chart objects
    ColumnChart mChartTraffic = null;
    ColumnChart mChartBundles = null;

    // chart data
    DataTable mDataChartTraffic = null;
    DataTable mDataChartBundles = null;
    
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
    }
    
    public HashMap<Long, Node> getNodes() {
        return mNodes;
    }
    
    @Override
    public void onResize(ResizeEvent event) {
        if (initialized) {
            Integer width = panelTraffic.getOffsetWidth();
            Double height = Double.valueOf(width) * Double.valueOf(9.0 / 16.0);
            mOptionsChart.setSize(width, height.intValue());
            
            if (mDataChartTraffic != null)
                mChartTraffic.draw(mDataChartTraffic, mOptionsChart);
            
            if (mDataChartBundles != null)
                mChartBundles.draw(mDataChartBundles, mOptionsChart);
        }
        
        // reload stats of nodes
        for (SessionNodeStatsWidget w : mNodeStats) {
            w.onResize(event);
        }
    }
    
    public void initialize(final Session session) {
        // load chart library
        initializeChart();
        
        // add resize handler
        Window.addResizeHandler(this);

        // load the list of nodes
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getNodes(session.id, new AsyncCallback<ArrayList<Node>>() {
            
            @Override
            public void onSuccess(ArrayList<Node> result) {
                // store the list of nodes
                for (Node n : result) {
                    mNodes.put(n.id, n);
                }
                
                // store session globally
                mSession = session;
                
                // create average node stats
                // TODO: replace by 'null' if average is working
                SessionNodeStatsWidget ns = new SessionNodeStatsWidget(session, result.get(0));
                mNodeStats.add(ns);
                containerNodeStats.add(ns);
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed to load node list
            }
        });
    }

    private void initializeChart() {
        // create chart options
        mOptionsChart = ColumnChart.Options.create();
        mOptionsChart.setLegend(LegendPosition.BOTTOM);
        mOptionsChart.setWidth(640);
        mOptionsChart.setHeight(480);
        
        Runnable onLoadCallbackColumn = new Runnable() {
            @Override
            public void run() {
                mChartTraffic = new ColumnChart();
                panelTraffic.add(mChartTraffic);
                //mChartTraffic.addSelectHandler(createSelectHandler(mChartTraffic));
                
                mChartBundles = new ColumnChart();
                panelBundles.add(mChartBundles);
                //mChartBundles.addSelectHandler(createSelectHandler(mChartBundles));
                
                updateStatsData(true);
                
                // set charts to initialized
                initialized = true;
            }
        };
        
        // Load the visualization api, passing the onLoadCallback to be called
        // when loading is done.
        VisualizationUtils.loadVisualizationApi(onLoadCallbackColumn, ColumnChart.PACKAGE);
    }
    
    public void updateStatsData(final boolean rebuild) {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getStatsLatest(mSession, new AsyncCallback<HashMap<Long,DataPoint>>() {
            
            @Override
            public void onSuccess(HashMap<Long, DataPoint> result) {
                transformStatsData(result, rebuild);
                
                // update charts
                mChartTraffic.draw(mDataChartTraffic, mOptionsChart);
                mChartBundles.draw(mDataChartBundles, mOptionsChart);
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed
            }
        });
    }
    
    public void refresh() {
        // reload stats data and redraw charts
        if (initialized) updateStatsData(false);
        
        // reload stats of nodes
        for (SessionNodeStatsWidget w : mNodeStats) {
            w.refresh();
        }
    }
    
    private void transformStatsData(HashMap<Long, DataPoint> result, boolean rebuild) {
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
        for (Entry<Long,DataPoint> e : result.entrySet()) {
            Long nodeId = e.getKey();
            DataPoint data = e.getValue();
            
            // generate the traffic columns
            if (mDataChartTraffic == null) {
                mDataChartTraffic = DataTable.create();
                
                mDataChartTraffic.addColumn(ColumnType.STRING, "Nodes");
                
                for (DataPoint.InterfaceStats iface : data.ifaces.values()) {
                    // skip "lo" and "eth1" interface
                    if (iface.name.equals("lo") || iface.name.equals("eth1")) continue;
                    
                    mDataChartTraffic.addColumn(ColumnType.NUMBER, iface.name + " (rx)");
                    mDataChartTraffic.addColumn(ColumnType.NUMBER, iface.name + " (tx)");
                }
                
                mDataChartTraffic.addRows(result.size());
            }
            
            if (rebuild)
                mDataChartTraffic.setValue(row, 0, mNodes.get(nodeId).name);
            
            Integer index = 1;
            
            for (DataPoint.InterfaceStats iface : data.ifaces.values()) {
                // skip "lo" and "eth1" interface
                if (iface.name.equals("lo") || iface.name.equals("eth1")) continue;

                mDataChartTraffic.setValue(row, index, iface.rx);
                mDataChartTraffic.setValue(row, index + 1, iface.tx);
                
                index += 2;
            }
            
            if (rebuild)
                mDataChartBundles.setValue(row, 0, mNodes.get(nodeId).name);
            
            mDataChartBundles.setValue(row, 1, data.bundlestats.received);
            mDataChartBundles.setValue(row, 2, data.bundlestats.transmitted);
            mDataChartBundles.setValue(row, 3, data.bundlestats.generated);
            
            row++;
        }
    }

//    /*** CHART DEMO CODE ***/
//    private SelectHandler createSelectHandler(final ColumnChart chart) {
//      return new SelectHandler() {
//        @Override
//        public void onSelect(SelectEvent event) {
//          String message = "";
//          
//          // May be multiple selections.
//          JsArray<Selection> selections = chart.getSelections();
//
//          for (int i = 0; i < selections.length(); i++) {
//            // add a new line for each selection
//            message += i == 0 ? "" : "\n";
//            
//            Selection selection = selections.get(i);
//
//            if (selection.isCell()) {
//              // isCell() returns true if a cell has been selected.
//              
//              // getRow() returns the row number of the selected cell.
//              int row = selection.getRow();
//              // getColumn() returns the column number of the selected cell.
//              int column = selection.getColumn();
//              message += "cell " + row + ":" + column + " selected";
//            } else if (selection.isRow()) {
//              // isRow() returns true if an entire row has been selected.
//              
//              // getRow() returns the row number of the selected row.
//              int row = selection.getRow();
//              message += "row " + row + " selected";
//            } else {
//              // unreachable
//              message += "Pie chart selections should be either row selections or cell selections.";
//              message += "  Other visualizations support column selections as well.";
//            }
//          }
//          
//          Window.alert(message);
//        }
//      };
//    }
}
