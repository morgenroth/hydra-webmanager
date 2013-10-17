package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.IconSize;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.LineChart;

import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionNodeStatsWidget extends Composite implements ResizeHandler {
    
    @UiField SimplePanel panelTraffic;
    @UiField SimplePanel panelBundles;
    @UiField SimplePanel panelClock;
    
    @UiField Heading headingNode;
    @UiField Heading headingTraffic;
    @UiField Heading headingBundles;
    @UiField Heading headingClock;
    
    @UiField IconAnchor buttonRemove;
    
    private Session mSession = null;
    private Node mNode = null;
    
    // chart objects
    LineChart mChartTraffic = null;
    LineChart mChartBundles = null;
    LineChart mChartClock = null;
    
    // chart data
    DataTable mDataChartTraffic = null;
    DataTable mDataChartBundles = null;
    DataTable mDataChartClock = null;
    
    // chart options
    LineChart.Options mOptionsChart = null;
    
    // variables for incremental data processing
    DataPoint mLastObj = null;
    Integer mLastRow = 0;
    
    // interface mapping
    HashMap<String, Integer> mInterfaceMap = new HashMap<String, Integer>();
    
    // is true if all charts are initialized
    boolean initialized = false;
    
    // removed listener
    private OnStatsRemovedListener mRemovedListener = null;

    private static SessionNodeStatsWidgetUiBinder uiBinder = GWT
            .create(SessionNodeStatsWidgetUiBinder.class);

    interface SessionNodeStatsWidgetUiBinder extends UiBinder<Widget, SessionNodeStatsWidget> {
    }
    
    public interface OnStatsRemovedListener {
        public void onStatsRemoved(Node n);
    }

    public SessionNodeStatsWidget(OnStatsRemovedListener listener, Session session, Node node) {
        initWidget(uiBinder.createAndBindUi(this));
        mRemovedListener = listener;
        mSession = session;
        mNode = node;
        
        // create chart options
        mOptionsChart = LineChart.Options.create();
        mOptionsChart.setLegend(LegendPosition.BOTTOM);
        mOptionsChart.setWidth(360);
        mOptionsChart.setHeight(480);
        
        if (mNode == null) {
            headingNode.setText("Average");
        } else {
            headingNode.setText("Node '" + mNode.name + "'");
        }
        
        // set remove button size
        buttonRemove.setIconSize(IconSize.LARGE);
    }
    
    @Override
    protected void onAttach() {
        super.onAttach();
        
        // invoke resize
        onResize(null);

        // load chart library
        initializeChart();
    }

    public void initializeChart() {
        Runnable onLoadCallbackLine = new Runnable() {
            @Override
            public void run() {
                // generate data table for 'traffic'
                mDataChartTraffic = DataTable.create();
                mDataChartTraffic.addColumn(ColumnType.STRING, "Time");
                
                // create and add traffic chart to panel
                mChartTraffic = new LineChart();
                panelTraffic.add(mChartTraffic);
                
                // generate data table for 'bundles'
                mDataChartBundles = DataTable.create();
                mDataChartBundles.addColumn(ColumnType.STRING, "Time");
                mDataChartBundles.addColumn(ColumnType.NUMBER, "Received");
                mDataChartBundles.addColumn(ColumnType.NUMBER, "Transmitted");
                mDataChartBundles.addColumn(ColumnType.NUMBER, "Generated");

                // create and add bundles chart to panel
                mChartBundles = new LineChart();
                panelBundles.add(mChartBundles);
                
                // generate data table for 'clock'
                mDataChartClock = DataTable.create();
                mDataChartClock.addColumn(ColumnType.STRING, "Time");
                mDataChartClock.addColumn(ColumnType.NUMBER, "Offset");

                // create and add clock chart to panel
                mChartClock = new LineChart();
                panelClock.add(mChartClock);
                
                // set charts to initialized
                initialized = true;
                
                updateStatsData();
            }
        };
        
        // Load the visualization api, passing the onLoadCallback to be called
        // when loading is done.
        VisualizationUtils.loadVisualizationApi(onLoadCallbackLine, LineChart.PACKAGE);
    }
    
    public void updateStatsData() {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync) GWT
                .create(MasterControlService.class);
        
        Date started = (mLastObj == null) ? mSession.started : mLastObj.time;
        
        if (started != null) {
            // get data since last session start
            mcs.getStatsData(mSession, mNode, started, null, new AsyncCallback<ArrayList<DataPoint>>() {
                @Override
                public void onSuccess(ArrayList<DataPoint> result) {
                    // transform result into data-table
                    transformStatsData(result);
                    redraw();
                }
    
                @Override
                public void onFailure(Throwable caught) {
                    // failed
                }
            });
        }
    }
    
    public void refresh() {
        // reload stats data and redraw charts
        if (initialized) updateStatsData();
    }
    
    private String getDurationString(long duration) {
        long hours = duration / 3600;
        long minutes = (duration % 3600) / 60;
        long seconds = duration % 60;
        
        NumberFormat f = NumberFormat.getFormat("#00");
        return f.format(hours) + ":" + f.format(minutes) + ":" + f.format(seconds);
    }
    
    private void transformStatsData(ArrayList<DataPoint> result) {
        // add more rows if necessary
        int rowsToAdd = (mLastObj == null) ? result.size() - 1 : result.size();
        if (rowsToAdd > 0) {
            mDataChartBundles.addRows(rowsToAdd);
            mDataChartTraffic.addRows(rowsToAdd);
            mDataChartClock.addRows(rowsToAdd);
        }
        
        // iterate through all the data
        for (DataPoint data : result)
        {
            // elapsed time
            Long elapsedSeconds = 0L;
            
            if (mSession.started != null) {
                // calculate the number of seconds since the progress has been started
                elapsedSeconds = (data.time.getTime() - mSession.started.getTime()) / 1000;
            }
            
            // skip the first data element
            if (mLastObj != null) {
                /**
                 * process traffic stats
                 */
                // set timestamp
                mDataChartTraffic.setValue(mLastRow, 0, getDurationString(elapsedSeconds));
                
                for (Map.Entry<String, DataPoint.InterfaceStats> e : data.ifaces.entrySet()) {
                    DataPoint.InterfaceStats iface = e.getValue();
                    
                    // skip "lo" and "eth1" interface
                    if (iface.name.equals("lo") || iface.name.equals("eth1")) continue;
                    
                    // get columns for rx and tx
                    int rx_index = 0, tx_index = 1;
                    
                    if (mInterfaceMap.containsKey(iface.name + "_rx")) {
                        rx_index = mInterfaceMap.get(iface.name + "_rx");
                        tx_index = mInterfaceMap.get(iface.name + "_tx");
                    } else {
                        rx_index = mDataChartTraffic.addColumn(ColumnType.NUMBER, iface.name + " (rx)");
                        tx_index = mDataChartTraffic.addColumn(ColumnType.NUMBER, iface.name + " (tx)");
                        mInterfaceMap.put(iface.name + "_rx", rx_index);
                        mInterfaceMap.put(iface.name + "_tx", tx_index);
                    }
    
                    // calculate relative data values
                    Long rx = iface.rx - mLastObj.ifaces.get(e.getKey()).rx;
                    Long tx = iface.tx - mLastObj.ifaces.get(e.getKey()).tx;
    
                    mDataChartTraffic.setValue(mLastRow, rx_index, rx);
                    mDataChartTraffic.setValue(mLastRow, tx_index, tx);
                }
                
                /**
                 * process dtn stats
                 */
                mDataChartBundles.setValue(mLastRow, 0, getDurationString(elapsedSeconds));
                mDataChartBundles.setValue(mLastRow, 1, data.bundlestats.received - mLastObj.bundlestats.received);
                mDataChartBundles.setValue(mLastRow, 2, data.bundlestats.transmitted - mLastObj.bundlestats.transmitted);
                mDataChartBundles.setValue(mLastRow, 3, data.bundlestats.generated - mLastObj.bundlestats.generated);
                
                /**
                 * process clock stats
                 */
                mDataChartClock.setValue(mLastRow, 0, getDurationString(elapsedSeconds));
                mDataChartClock.setValue(mLastRow, 1, data.clock.offset);
                
                // increment row number
                mLastRow++;
            }

            // store data of the last item for incremental processing
            mLastObj = data;
        }
    }
    
    public void onSessionUpdated(Session s) {
        mSession = s;
        refresh();
    }

    @Override
    public void onResize(ResizeEvent event) {
        Integer width = panelTraffic.getOffsetWidth();
        Double height = Double.valueOf(width) * Double.valueOf(11.0 / 16.0);
        
        if (width > 0) {
            mOptionsChart.setSize(width, height.intValue());
    
            // redraw local charts
            redraw();
        }
    }

    private void redraw() {
        // do not redraw until the chart library has been initialized
        if (!initialized) return;
    
        // redraw traffic chart
        mChartTraffic.draw(mDataChartTraffic, mOptionsChart);
    
        // redraw bundles chart
        mChartBundles.draw(mDataChartBundles, mOptionsChart);
        
        // redraw clock chart
        mChartClock.draw(mDataChartClock, mOptionsChart);
    }
    
    @UiHandler("buttonRemove")
    public void onClickRemove(ClickEvent e) {
        this.removeFromParent();
        if (mRemovedListener != null)
            mRemovedListener.onStatsRemoved(mNode);
    }
}
