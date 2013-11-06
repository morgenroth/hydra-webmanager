package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Date;

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
import com.google.gwt.visualization.client.DataView;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.LineChart;

import de.tubs.cs.ibr.hydra.webmanager.client.stats.StatsJso;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionNodeStatsWidget extends Composite implements ResizeHandler {
    
    @UiField SimplePanel panelTraffic;
    @UiField SimplePanel panelBundles;
    @UiField SimplePanel panelClock;
    @UiField SimplePanel panelTimeSync;
    
    @UiField Heading headingNode;
    @UiField Heading headingTraffic;
    @UiField Heading headingBundles;
    @UiField Heading headingClock;
    @UiField Heading headingTimeSync;
    
    @UiField IconAnchor buttonRemove;
    
    private Session mSession = null;
    private Node mNode = null;
    
    // chart objects
    LineChart mChartTraffic = null;
    LineChart mChartBundles = null;
    LineChart mChartClock = null;
    LineChart mChartTimeSync = null;
    
    // chart data
    DataTable mData = null;
    
    // chart views
    DataView mViewTraffic = null;
    DataView mViewBundles = null;
    DataView mViewClock = null;
    DataView mViewTimeSync = null;
    
    // chart options
    LineChart.Options mOptionsChart = null;
    
    // variables for incremental data processing
    DataPoint mLastObj = null;
    Integer mLastRow = 0;
    
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
                mData = DataTable.create();
                mData.addColumn(ColumnType.STRING, "Time");
                mData.addColumn(ColumnType.NUMBER, "TCP (in)");
                mData.addColumn(ColumnType.NUMBER, "TCP (out)");
                mData.addColumn(ColumnType.NUMBER, "UDP (in)");
                mData.addColumn(ColumnType.NUMBER, "UDP (out)");
                mData.addColumn(ColumnType.NUMBER, "Received");
                mData.addColumn(ColumnType.NUMBER, "Transmitted");
                mData.addColumn(ColumnType.NUMBER, "Generated");
                mData.addColumn(ColumnType.NUMBER, "Offset");
                mData.addColumn(ColumnType.NUMBER, "Rating");

                // create and add traffic chart to panel
                mChartTraffic = new LineChart();
                panelTraffic.add(mChartTraffic);
                
                // create and add bundles chart to panel
                mChartBundles = new LineChart();
                panelBundles.add(mChartBundles);

                // create and add clock chart to panel
                mChartClock = new LineChart();
                panelClock.add(mChartClock);

                // create and add timesync chart to panel
                mChartTimeSync = new LineChart();
                panelTimeSync.add(mChartTimeSync);
                
                // create different views
                mViewTraffic = DataView.create(mData);
                mViewTraffic.setColumns(new int[] { 0, 1, 2, 3, 4 });
                
                mViewBundles = DataView.create(mData);
                mViewBundles.setColumns(new int[] { 0, 5, 6, 7 });

                mViewClock = DataView.create(mData);
                mViewClock.setColumns(new int[] { 0, 8 });

                mViewTimeSync = DataView.create(mData);
                mViewTimeSync.setColumns(new int[] { 0, 9 });
                
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
            mData.addRows(rowsToAdd);
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
                // convert json to object
                StatsJso stats = StatsJso.create(data.json);
                StatsJso last_stats = StatsJso.create(mLastObj.json);
                
                /**
                 * process traffic stats
                 */
                mData.setValue(mLastRow, 0, getDurationString(elapsedSeconds));
                mData.setValue(mLastRow, 1, stats.getTraffic().getInTcpByte() - last_stats.getTraffic().getInTcpByte());
                mData.setValue(mLastRow, 2, stats.getTraffic().getOutTcpByte() - last_stats.getTraffic().getOutTcpByte());
                mData.setValue(mLastRow, 3, stats.getTraffic().getInUdpByte() - last_stats.getTraffic().getInUdpByte());
                mData.setValue(mLastRow, 4, stats.getTraffic().getOutUdpByte() - last_stats.getTraffic().getOutUdpByte());
                
                /**
                 * process dtn stats
                 */
                mData.setValue(mLastRow, 5, stats.getDtnd().getBundles().getReceived() - last_stats.getDtnd().getBundles().getReceived());
                mData.setValue(mLastRow, 6, stats.getDtnd().getBundles().getTransmitted() - last_stats.getDtnd().getBundles().getTransmitted());
                mData.setValue(mLastRow, 7, stats.getDtnd().getBundles().getGenerated() - last_stats.getDtnd().getBundles().getTransmitted());
                
                /**
                 * process clock stats
                 */
                mData.setValue(mLastRow, 8, stats.getClock().getOffset());
                
                /**
                 * process time-sync stats
                 */
                mData.setValue(mLastRow, 9, stats.getDtnd().getTimeSync().getRating());
                
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
        mChartTraffic.draw(mViewTraffic, mOptionsChart);
    
        // redraw bundles chart
        mChartBundles.draw(mViewBundles, mOptionsChart);
        
        // redraw clock chart
        mChartClock.draw(mViewClock, mOptionsChart);
        
        // redraw time-sync chart
        mChartTimeSync.draw(mViewTimeSync, mOptionsChart);
    }
    
    @UiHandler("buttonRemove")
    public void onClickRemove(ClickEvent e) {
        this.removeFromParent();
        if (mRemovedListener != null)
            mRemovedListener.onStatsRemoved(mNode);
    }
}
