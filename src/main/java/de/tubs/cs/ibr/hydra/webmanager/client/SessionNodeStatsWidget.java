package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.Date;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.NavLink;
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
import com.google.gwt.visualization.client.visualizations.corechart.AxisOptions;
import com.google.gwt.visualization.client.visualizations.corechart.LineChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;

import de.tubs.cs.ibr.hydra.webmanager.client.stats.StatsJso;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionNodeStatsWidget extends Composite implements ResizeHandler {
    
    @UiField SimplePanel panelChart;
    
    @UiField Heading headingNode;
    
    @UiField IconAnchor buttonRemove;
    
    @UiField NavLink linkIpTraffic;
    @UiField NavLink linkDtnTraffic;
    @UiField NavLink linkClockOffset;
    @UiField NavLink linkDtnClockOffset;
    @UiField NavLink linkClockRating;
    @UiField NavLink linkUptime;
    @UiField NavLink linkStorageSize;
    
    private Session mSession = null;
    private Node mNode = null;
    
    // chart objects
    LineChart mChart = null;
    
    // chart data
    DataTable mData = null;
    
    // chart views
    DataView[] mView = { null, null, null, null, null, null, null };
    
    // currently selected chart
    int mCurrentView = 0;
    
    // chart options
    Options[] mOptions = { null, null, null, null, null, null, null };
    
    // variables for incremental data processing
    DataPoint mLastObj = null;
    Long mLastTime = null;
    Integer mLastRow = 0;
    
    // maximum number of rows to display
    private static final int MAX_ROWS = 60;
    
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
        
        // initialize chart options
        updateChartOptions();
        
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
                mView[2].setColumns(new int[] { 0, 10 });
                mView[3].setColumns(new int[] { 0, 12 });
                mView[4].setColumns(new int[] { 0, 8 });
                mView[5].setColumns(new int[] { 0, 9 });
                mView[6].setColumns(new int[] { 0, 11 });

                // create default chart
                mChart = new LineChart(mView[0], mOptions[0]);
                panelChart.add(mChart);
                
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
            GWT.log("starting get");
            mcs.getStatsData(mSession, mNode, started, null, new AsyncCallback<ArrayList<DataPoint>>() {
                @Override
                public void onSuccess(ArrayList<DataPoint> result) {
                    // transform result into data-table
                    GWT.log("starting transform");
                    transformStatsData(result);
                    GWT.log("done transform");
                    GWT.log("starting redraw");
                    redraw(mCurrentView);
                    GWT.log("done redraw");
                }
    
                @Override
                public void onFailure(Throwable caught) {
                    // failed
                }
            });
            GWT.log("done get");
        }
    }
    
    public void refresh() {
        // reload stats data and redraw charts
        if (initialized) updateStatsData();
    }
    
    private static String formatDuration(long duration) {
        long hours = duration / 3600;
        long minutes = (duration % 3600) / 60;
        long seconds = duration % 60;
        
        NumberFormat f = NumberFormat.getFormat("#00");
        return f.format(hours) + ":" + f.format(minutes) + ":" + f.format(seconds);
    }
    
    private static Double normalize(Double currentValue, Double previousValue, long interval) {
        return (currentValue - previousValue) / Double.valueOf(interval);
    }
    
    private static Double normalize(int currentValue, int previousValue, long interval) {
        return normalize(Double.valueOf(currentValue), Double.valueOf(previousValue), interval);
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
                
                long diffSeconds = elapsedSeconds - mLastTime;
                
                /**
                 * process traffic stats
                 */
                mData.setValue(mLastRow, 0, formatDuration(elapsedSeconds));
                mData.setValue(mLastRow, 1, normalize(stats.getTraffic().getInTcpByte(), last_stats.getTraffic().getInTcpByte(), diffSeconds));
                mData.setValue(mLastRow, 2, normalize(stats.getTraffic().getOutTcpByte(), last_stats.getTraffic().getOutTcpByte(), diffSeconds));
                mData.setValue(mLastRow, 3, normalize(stats.getTraffic().getInUdpByte(),last_stats.getTraffic().getInUdpByte(), diffSeconds));
                mData.setValue(mLastRow, 4, normalize(stats.getTraffic().getOutUdpByte(), last_stats.getTraffic().getOutUdpByte(), diffSeconds));
                
                /**
                 * process dtn stats
                 */
                mData.setValue(mLastRow, 5, normalize(stats.getDtnd().getBundles().getReceived(), last_stats.getDtnd().getBundles().getReceived(), diffSeconds));
                mData.setValue(mLastRow, 6, normalize(stats.getDtnd().getBundles().getTransmitted(), last_stats.getDtnd().getBundles().getTransmitted(), diffSeconds));
                mData.setValue(mLastRow, 7, normalize(stats.getDtnd().getBundles().getGenerated(), last_stats.getDtnd().getBundles().getTransmitted(), diffSeconds));
                
                mData.setValue(mLastRow, 8, stats.getDtnd().getInfo().getUptime());
                mData.setValue(mLastRow, 9, stats.getDtnd().getInfo().getStorageSize());
                
                /**
                 * process clock stats
                 */
                mData.setValue(mLastRow, 10, stats.getClock().getOffset());
                mData.setValue(mLastRow, 11, stats.getDtnd().getTimeSync().getOffset());
                mData.setValue(mLastRow, 12, stats.getDtnd().getTimeSync().getRating());
                
                // increment row number
                mLastRow++;
            }

            // store data of the last item for incremental processing
            mLastObj = data;
            mLastTime = elapsedSeconds;
        }
    }
    
    public void onSessionUpdated(Session s) {
        mSession = s;
        refresh();
    }

    @Override
    public void onResize(ResizeEvent event) {
        updateChartOptions();
        
        // redraw local charts
        redraw(mCurrentView);
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
                        vAxisOptions.setTitle("bytes per second");
                        break;
                    case 1:
                        mOptions[i].setTitle("DTN Traffic");
                        vAxisOptions.setTitle("bundles per second");
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
                    case 6:
                        mOptions[i].setTitle("DTN Clock Offset");
                        vAxisOptions.setTitle("seconds");
                        break;
                    default:
                        // no title
                        break;
                }
                
                mOptions[i].setVAxisOptions(vAxisOptions);
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

    private void redraw(int i) {
        GWT.log("redraw start");
        // do not redraw until the chart library has been initialized
        if (!initialized) return;
        
        if (mData.getNumberOfRows() > 0) {
            // adjust views to show only last X entries
            int dataRows = mData.getNumberOfRows() - 1;
            int offset = (dataRows < MAX_ROWS) ? 0 : dataRows - MAX_ROWS;
            mView[i].setRows(offset, dataRows);
        }
    
        // redraw charts
        mChart.draw(mView[i], mOptions[i]);
        GWT.log("redraw end");
    }
    
    @UiHandler("buttonRemove")
    public void onClickRemove(ClickEvent e) {
        this.removeFromParent();
        if (mRemovedListener != null)
            mRemovedListener.onStatsRemoved(mNode);
    }
    
    @UiHandler("linkIpTraffic")
    public void onNavIpTrafficClick(ClickEvent evt) {
        GWT.log("click 1");
        mCurrentView = 0;
        redraw(mCurrentView);
    }
    
    @UiHandler("linkDtnTraffic")
    public void onNavDtnTrafficClick(ClickEvent evt) {
        GWT.log("click 2");
        mCurrentView = 1;
        redraw(mCurrentView);
    }
    
    @UiHandler("linkClockOffset")
    public void onNavClockOffsetClick(ClickEvent evt) {
        GWT.log("click 3");
        mCurrentView = 2;
        redraw(mCurrentView);
    }
    
    @UiHandler("linkClockRating")
    public void onNavClockRatingClick(ClickEvent evt) {
        GWT.log("click 4");
        mCurrentView = 3;
        redraw(mCurrentView);
    }
    
    @UiHandler("linkUptime")
    public void onNavUptimeClick(ClickEvent evt) {
        mCurrentView = 4;
        GWT.log("click 5");
        redraw(mCurrentView);
    }
    
    @UiHandler("linkStorageSize")
    public void onNavStorageSizeClick(ClickEvent evt) {
        GWT.log("click 5");
        mCurrentView = 5;
        redraw(mCurrentView);
    }
    
    @UiHandler("linkDtnClockOffset")
    public void onNavDtnClockOffsetClick(ClickEvent evt) {
        GWT.log("click 6");
        mCurrentView = 6;
        redraw(mCurrentView);
    }
}
