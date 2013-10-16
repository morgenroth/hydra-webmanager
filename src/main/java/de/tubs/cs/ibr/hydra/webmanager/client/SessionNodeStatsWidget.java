package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
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

public class SessionNodeStatsWidget extends Composite {
    
    @UiField SimplePanel panelTraffic;
    @UiField SimplePanel panelBundles;
    @UiField SimplePanel panelClock;
    
    @UiField Heading headingNode;
    @UiField Heading headingTraffic;
    @UiField Heading headingBundles;
    @UiField Heading headingClock;
    
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
    Integer mLastRow = -1;
    
    // is true if all charts are initialized
    boolean initialized = false;

    private static SessionNodeStatsWidgetUiBinder uiBinder = GWT
            .create(SessionNodeStatsWidgetUiBinder.class);

    interface SessionNodeStatsWidgetUiBinder extends UiBinder<Widget, SessionNodeStatsWidget> {
    }

    public SessionNodeStatsWidget(Session session, Node node) {
        initWidget(uiBinder.createAndBindUi(this));
        mSession = session;
        mNode = node;
    }
    
    @Override
    protected void onAttach() {
        super.onAttach();

        // initialize this widget
        initialize();
    }

    public void initialize() {
        if (mNode == null) {
            headingNode.setText("Average");
        } else {
            headingNode.setText("Node '" + mNode.name + "'");
        }
        
        // create chart options
        mOptionsChart = LineChart.Options.create();
        mOptionsChart.setLegend(LegendPosition.BOTTOM);
        mOptionsChart.setWidth(360);
        mOptionsChart.setHeight(320);
        
        Runnable onLoadCallbackLine = new Runnable() {
            @Override
            public void run() {
                // create and add traffic chart to panel
                mChartTraffic = new LineChart();
                panelTraffic.add(mChartTraffic);

                // create and add bundles chart to panel
                mChartBundles = new LineChart();
                panelBundles.add(mChartBundles);

                // create and add clock chart to panel
                mChartClock = new LineChart();
                panelClock.add(mChartClock);
                
                updateStatsData(true);
                
                // set charts to initialized
                initialized = true;
            }
        };
        
        // Load the visualization api, passing the onLoadCallback to be called
        // when loading is done.
        VisualizationUtils.loadVisualizationApi(onLoadCallbackLine, LineChart.PACKAGE);
    }
    
    public void updateStatsData(final boolean rebuild) {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync) GWT
                .create(MasterControlService.class);
        
        Date started = (rebuild) ? mSession.started : ((mLastObj == null) ? null : mLastObj.time);
        if (started == null) started = new Date();

        // get data since last session start
        mcs.getStatsData(mSession, mNode, started, null,
                new AsyncCallback<ArrayList<DataPoint>>() {

                    @Override
                    public void onSuccess(ArrayList<DataPoint> result) {
                        if (rebuild) {
                            mDataChartTraffic = null;
                            mDataChartBundles = null;
                            mDataChartClock = null;
                            
                            // reset last object
                            mLastObj = null;
                            
                            // set the current number of rows to zero
                            mLastRow = -1;
                        }
                        
                        // do not process empty data-sets
                        if (result.isEmpty()) return;
                        
                        // transform result into data-table
                        transformStatsData(result);
                        
                        if (mDataChartTraffic != null) {
                            // update traffic chart
                            mChartTraffic.draw(mDataChartTraffic, mOptionsChart);
                        }

                        if (mDataChartBundles != null) {
                            // update bundles chart
                            mChartBundles.draw(mDataChartBundles, mOptionsChart);
                        }
                        
                        if (mDataChartClock != null) {
                             // update clock chart
                            mChartClock.draw(mDataChartClock, mOptionsChart);
                        }
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
    }
    
    private void transformStatsData(ArrayList<DataPoint> result) {
        // get the current number of rows
        if (mDataChartTraffic != null) {
            // for incremental updates we need to add new rows here
            mDataChartTraffic.addRows(result.size());
            mDataChartBundles.addRows(result.size());
            mDataChartClock.addRows(result.size());
        }
        
        // iterate through all the data
        for (DataPoint data : result)
        {
            /**
             * process traffic stats
             */
            // generate the traffic columns
            if (mDataChartTraffic == null)
            {
                mDataChartTraffic = DataTable.create();
                
                mDataChartTraffic.addColumn(ColumnType.DATETIME, "Time");
                
                // columns are added dynamically based on the available interfaces
                for (DataPoint.InterfaceStats iface : data.ifaces.values()) {
                    // skip "lo" and "eth1" interface
                    if (iface.name.equals("lo") || iface.name.equals("eth1")) continue;
                    
                    mDataChartTraffic.addColumn(ColumnType.NUMBER, iface.name + " (rx)");
                    mDataChartTraffic.addColumn(ColumnType.NUMBER, iface.name + " (tx)");
                }
                
                // add enough rows for the first data-set
                // "-1" because of the first skipped row
                mDataChartTraffic.addRows(result.size() - 1);
            }
            else
            {
                // set timestamp
                mDataChartTraffic.setValue(mLastRow, 0, data.time);
                
                // index for the row data
                Integer index = 1;
    
                for (Map.Entry<String, DataPoint.InterfaceStats> e : data.ifaces.entrySet()) {
                    DataPoint.InterfaceStats iface = e.getValue();
                    
                    // skip "lo" and "eth1" interface
                    if (iface.name.equals("lo") || iface.name.equals("eth1")) continue;
    
                    // calculate relative data values
                    Long rx = iface.rx - mLastObj.ifaces.get(e.getKey()).rx;
                    Long tx = iface.tx - mLastObj.ifaces.get(e.getKey()).tx;
    
                    mDataChartTraffic.setValue(mLastRow, index, rx);
                    mDataChartTraffic.setValue(mLastRow, index + 1, tx);
                    
                    index += 2;
                }
            }
            
            /**
             * process dtn stats
             */
            // generate data-set if necessary
            if (mDataChartBundles == null)
            {
                mDataChartBundles = DataTable.create();
                mDataChartBundles.addColumn(ColumnType.NUMBER, "Received");
                mDataChartBundles.addColumn(ColumnType.NUMBER, "Transmitted");
                mDataChartBundles.addColumn(ColumnType.NUMBER, "Generated");
                mDataChartBundles.addRows(result.size() - 1);
            }
            else
            {
                mDataChartBundles.setValue(mLastRow, 0, data.bundlestats.received - mLastObj.bundlestats.received);
                mDataChartBundles.setValue(mLastRow, 1, data.bundlestats.transmitted - mLastObj.bundlestats.transmitted);
                mDataChartBundles.setValue(mLastRow, 2, data.bundlestats.generated - mLastObj.bundlestats.generated);
            }
            
            /**
             * process clock stats
             */
            // generate data-set if necessary
            if (mDataChartClock == null)
            {
                mDataChartClock = DataTable.create();
                mDataChartClock.addColumn(ColumnType.NUMBER, "Offset");
                mDataChartClock.addRows(result.size() - 1);
            }
            else
            {
                mDataChartClock.setValue(mLastRow, 0, data.clock.offset);
            }
            
            // store data of the last item for incremental processing
            mLastObj = data;
            
            // increment row number
            mLastRow++;
        }
    }
}
