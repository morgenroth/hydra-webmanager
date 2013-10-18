package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

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
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.visualizations.MapVisualization;
import com.google.gwt.visualization.client.visualizations.MapVisualization.Type;

import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.GeoCoordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionMapWidget extends Composite implements ResizeHandler {

    private static SessionMapWidgetUiBinder uiBinder = GWT.create(SessionMapWidgetUiBinder.class);

    interface SessionMapWidgetUiBinder extends UiBinder<Widget, SessionMapWidget> {
    }
    
    @UiField SimplePanel panelMap;

    private GeoCoordinates mFix = null;
    private HashMap<Long, Node> mNodes = new HashMap<Long, Node>();
    private Session mSession = null;
    
    // chart object
    MapVisualization mMapChart = null;

    // chart data
    DataTable mMapData = null;
    
    // chart options
    MapVisualization.Options mOptions = null;
    
    // is true if all charts are initialized
    boolean initialized = false;

    public SessionMapWidget() {
        initWidget(uiBinder.createAndBindUi(this));
        
        // create chart options
        mOptions = MapVisualization.Options.create();
        mOptions.setMapType(Type.NORMAL);
        mOptions.setEnableScrollWheel(true);
        mOptions.setShowTip(true);
        
        // add resize handler
        Window.addResizeHandler(this);
    }
    
    public void onSessionUpdated(Session s) {
        mSession = s;
    }
    
    public void initialize(final Session session) {
        // store session globally
        mSession = session;
        
        // generate a fix coordinate for map projection (Arktis)
        mFix = new GeoCoordinates(-82.142451,93.779297);
        
        // load chart library
        initializeChart();
        
        // load the list of nodes
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getNodes(session.id, new AsyncCallback<ArrayList<Node>>() {
            
            @Override
            public void onSuccess(ArrayList<Node> result) {
                // store the list of nodes
                for (Node n : result) {
                    mNodes.put(n.id, n);
                }
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed to load node list
            }
        });
    }
    
    private void initializeChart() {
        Runnable onLoadCallbackColumn = new Runnable() {
            @Override
            public void run() {
                mMapData = DataTable.create();
                mMapData.addColumn(ColumnType.NUMBER, "Lat");
                mMapData.addColumn(ColumnType.NUMBER, "Lon");
                mMapData.addColumn(ColumnType.STRING, "Node");
                
                mMapChart = new MapVisualization(mMapData, mOptions, "640px", "480px");
                panelMap.add(mMapChart);
                
                // set charts to initialized
                initialized = true;
                
                updateData();
            }
        };
        
        // Load the visualization api, passing the onLoadCallback to be called
        // when loading is done.
        VisualizationUtils.loadVisualizationApi(onLoadCallbackColumn, MapVisualization.PACKAGE);
    }
    
    public void updateData() {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getStatsLatest(mSession, new AsyncCallback<HashMap<Long,DataPoint>>() {
            
            @Override
            public void onSuccess(HashMap<Long, DataPoint> result) {
                transformData(result);
                redraw();
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed
            }
        });
    }
    
    private void transformData(HashMap<Long, DataPoint> result) {
        // add more rows if necessary
        int nor = mMapData.getNumberOfRows();
        if (nor < result.size()) {
            mMapData.addRows(result.size() - nor);
        }
        
        Integer row = 0;
        
        for (Entry<Long,DataPoint> e : result.entrySet()) {
            Long nodeId = e.getKey();
            DataPoint data = e.getValue();
            
            GeoCoordinates geo = data.coord.getGeoCoordinates(mFix);
            
            mMapData.setValue(row, 0, geo.getLat());
            mMapData.setValue(row, 1, geo.getLon());
            mMapData.setValue(row, 2, mNodes.get(nodeId).name);
            
            row++;
        }
    }
    
    public void refresh() {
        // reload stats data and redraw charts
        if (initialized) updateData();
    }

    @Override
    public void onResize(ResizeEvent event) {
        Integer width = panelMap.getOffsetWidth();
        Double height = Double.valueOf(width) * Double.valueOf(9.0 / 16.0);
        
        if (width > 0) {
            mMapChart.setSize(width + "px", height.intValue() + "px");
        
            // redraw local charts
            redraw();
        }
    }
    
    private void redraw() {
        // do not redraw until the chart library has been initialized
        if (!initialized) return;

        // redraw map chart
        mMapChart.draw(mMapData, mOptions);
    }
}
