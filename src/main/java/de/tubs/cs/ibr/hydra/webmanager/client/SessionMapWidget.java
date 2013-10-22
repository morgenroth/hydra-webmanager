package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import com.google.gwt.ajaxloader.client.AjaxLoader;
import com.google.gwt.ajaxloader.client.AjaxLoader.AjaxLoaderOptions;
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
import com.google.maps.gwt.client.GoogleMap;
import com.google.maps.gwt.client.LatLng;
import com.google.maps.gwt.client.MapOptions;
import com.google.maps.gwt.client.MapTypeId;
import com.google.maps.gwt.client.MarkerImage;
import com.google.maps.gwt.client.Point;

import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.GeoCoordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Link;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionMapWidget extends Composite implements ResizeHandler, EventListener {

    private static SessionMapWidgetUiBinder uiBinder = GWT.create(SessionMapWidgetUiBinder.class);

    interface SessionMapWidgetUiBinder extends UiBinder<Widget, SessionMapWidget> {
    }
    
    @UiField SimplePanel panelMap;

    private GeoCoordinates mFix = null;
    private Session mSession = null;
    
    private MarkerImage mBlueIcon = null;
    private MarkerImage mRedIcon = null;
    private MarkerImage mGreenIcon = null;
    
    // list for nodes
    HashMap<Long, MapNode> mNodes = new HashMap<Long, MapNode>();
    
    // list for links
    HashSet<Link> mLinks = new HashSet<Link>();
    
    // map object
    GoogleMap mMap = null;
    MapOptions mOptions = null;
    
    // is true if all charts are initialized
    boolean initialized = false;

    public SessionMapWidget() {
        initWidget(uiBinder.createAndBindUi(this));
        
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
        //mFix = new GeoCoordinates(-82.142451, 93.779297);
        mFix = new GeoCoordinates(52.16, 10.32);
        
        // load map library
        initializeMaps();
    }
    
    private void initializeMaps() {
        AjaxLoaderOptions options = AjaxLoaderOptions.newInstance();
        options.setOtherParms("sensor=false");
        Runnable callback = new Runnable() {
          public void run() {
              LatLng center = LatLng.create(mFix.getLat(), mFix.getLon());
              
              // create map options
              mOptions = MapOptions.create();
              mOptions.setZoom(14.0);
              mOptions.setCenter(center);
              mOptions.setMapTypeId(MapTypeId.ROADMAP);
              
              // create the map widget
              mMap = GoogleMap.create(panelMap.getElement(), mOptions);
              
              // instantiate the node marker resources
              MapMarker res = GWT.create(MapMarker.class);
              
              // define anchor point for the markers
              Point anchor = Point.create(res.blue().getWidth() / 2, res.blue().getHeight() / 2);
              
              mBlueIcon = MarkerImage.create(res.blue().getSafeUri().asString(), null, null, anchor);
              mRedIcon = MarkerImage.create(res.red().getSafeUri().asString(), null, null, anchor);
              mGreenIcon = MarkerImage.create(res.green().getSafeUri().asString(), null, null, anchor);
              
              // set charts to initialized
              initialized = true;
              
              // resize the map frame
              onResize(null);
              
              loadNodes();
          }
        };
        AjaxLoader.loadApi("maps", "3", callback, options);
    }
    
    private void loadNodes() {
        // load the list of nodes
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getNodes(mSession.id, new AsyncCallback<ArrayList<Node>>() {
            
            @Override
            public void onSuccess(ArrayList<Node> result) {
                // store the list of nodes
                for (Node n : result) {
                    // assign nodes range
                    n.range = mSession.range;
                    
                    // create a map node
                    mNodes.put(n.id, new MapNode(SessionMapWidget.this, n));
                }
                
                updateStats();
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed to load node list
            }
        });
    }
    
    private void updateLinks() {
        // load the list of links
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getLinks(mSession.id, new AsyncCallback<ArrayList<Link>>() {
            
            @Override
            public void onSuccess(ArrayList<Link> result) {
                if (result == null) return;
                
                // remove all active links
                mLinks.removeAll(result);
                
                for (Link l : mLinks) {
                    // remove link
                    MapNode source = mNodes.get(l.source.id);
                    MapNode target = mNodes.get(l.target.id);
                    source.setLink(target, false);
                }
                
                mLinks.clear();
                mLinks.addAll(result);
                
                for (Link l : mLinks) {
                    // add links
                    MapNode source = mNodes.get(l.source.id);
                    MapNode target = mNodes.get(l.target.id);
                    source.setLink(target, true);
                }
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed to load node list
            }
        });
    }
    
    public void updateStats() {
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getStatsLatest(mSession, new AsyncCallback<HashMap<Long,DataPoint>>() {
            
            @Override
            public void onSuccess(HashMap<Long, DataPoint> result) {
                transformData(result);
                updateLinks();
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed
            }
        });
    }
    
    private void transformData(HashMap<Long, DataPoint> result) {
        for (Entry<Long,DataPoint> e : result.entrySet()) {
            Long nodeId = e.getKey();
            MapNode n = mNodes.get(nodeId);
            
            if (n == null) continue;

            // assign data point
            n.setData(e.getValue(), mBlueIcon, mMap);
            
            // set node position
            n.setPosition(n.getData().coord, mFix);
        }
    }
    
    public void refresh() {
        // reload stats data and redraw charts
        if (initialized) updateStats();
    }

    @Override
    public void onResize(ResizeEvent event) {
        Integer width = panelMap.getOffsetWidth();
        Double height = Double.valueOf(width) * Double.valueOf(9.0 / 16.0);
        
        if (width > 0) {
            panelMap.setHeight(height.intValue() + "px");
        }
    }
    
    public void onNodeClick(MapNode n) {
        // TODO: show node information
    }

    @Override
    public void onEventRaised(Event evt) {
        if (evt.equals(EventType.SESSION_LINK_UP)) {
            // TODO: show new link
        }
        else if (evt.equals(EventType.SESSION_LINK_DOWN)) {
            // TODO: hide link
        }
        else if (evt.equals(EventType.SESSION_NODE_MOVED)) {
            // TODO: move node
        }
    }
}
