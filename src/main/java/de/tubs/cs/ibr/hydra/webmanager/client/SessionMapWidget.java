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

import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
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
    HashSet<MapLink> mLinks = new HashSet<MapLink>();
    
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
                
                // set of disappeared links
                HashSet<MapLink> disappeared = new HashSet<MapLink>(mLinks);
                
                // remove all active links
                disappeared.removeAll(result);
                
                for (MapLink ml : disappeared) {
                    // remove link
                    ml.hide();
                }
                
                // remove all disappeared links
                mLinks.removeAll(disappeared);
                
                for (Link l : result) {
                    if (mLinks.contains(l)) continue;
                    
                    // create one map link for each link
                    MapNode source = mNodes.get(l.source.id);
                    MapNode target = mNodes.get(l.target.id);

                    MapLink ml = new MapLink(source, target);
                    ml.show(mMap);
                    mLinks.add(ml);
                }
                
                // set charts to initialized
                initialized = true;
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
                if (!initialized) updateLinks();
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
            if (n.getPosition() == null) {
                setNodePosition(n, n.getData().coord);
            }
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
            // parse link data
            Long source_id = evt.getExtraLong(EventType.EXTRA_LINK_SOURCE_ID);
            Long target_id = evt.getExtraLong(EventType.EXTRA_LINK_TARGET_ID);
            
            if ((source_id == null) || (target_id == null)) return;
            
            MapNode source = mNodes.get(source_id);
            MapNode target = mNodes.get(target_id);
            
            if ((source == null) || (target == null)) return;
            
            // create a new link-object
            MapLink ml = new MapLink(source, target);
            
            // activate the link
            if (!mLinks.contains(ml)) {
                ml.show(mMap);
                mLinks.add(ml);
            }
        }
        else if (evt.equals(EventType.SESSION_LINK_DOWN)) {
            // parse link data
            Long source_id = evt.getExtraLong(EventType.EXTRA_LINK_SOURCE_ID);
            Long target_id = evt.getExtraLong(EventType.EXTRA_LINK_TARGET_ID);
            
            if ((source_id == null) || (target_id == null)) return;
            
            MapNode source = mNodes.get(source_id);
            MapNode target = mNodes.get(target_id);
            
            if ((source == null) || (target == null)) return;
            
            // disable the link
            for (MapLink ml : mLinks) {
                if (ml.hasSource(source) && ml.hasTarget(target)) {
                    ml.hide();
                    mLinks.remove(ml);
                    break;
                }
            }
        }
        else if (evt.equals(EventType.SESSION_NODE_MOVED)) {
            // get the moved node
            Long node_id = evt.getExtraLong(EventType.EXTRA_NODE_ID);
            MapNode n = mNodes.get(node_id);
            
            if (n == null) return;

            // get new position
            Double x = evt.getExtraDouble(EventType.EXTRA_POSITION_X);
            Double y = evt.getExtraDouble(EventType.EXTRA_POSITION_Y);
            
            if ((x == null) || (y == null)) return;
            
            Coordinates coord = new Coordinates(x, y);
            
            setNodePosition(n, coord);
        }
    }
    
    private void setNodePosition(MapNode n, Coordinates coord) {
        // set node position
        n.setPosition(coord, mFix);
        
        // adjust link position
        for (MapLink ml : mLinks) {
            if (ml.hasSource(n) || ml.hasTarget(n)) {
                ml.show(mMap);
            }
        }
    }
}
