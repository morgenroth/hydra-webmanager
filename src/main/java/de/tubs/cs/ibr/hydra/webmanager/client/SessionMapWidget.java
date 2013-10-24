package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.HashMap;
import java.util.HashSet;

import com.google.gwt.ajaxloader.client.AjaxLoader;
import com.google.gwt.ajaxloader.client.AjaxLoader.AjaxLoaderOptions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
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

import de.tubs.cs.ibr.hydra.webmanager.shared.GeoCoordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Link;
import de.tubs.cs.ibr.hydra.webmanager.shared.MapDataSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionMapWidget extends Composite implements ResizeHandler {

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
              
              // set maps to initialized
              initialized = true;
              
              // resize the map frame
              onResize(null);
          }
        };
        AjaxLoader.loadApi("maps", "3", callback, options);
    }
    
    private void update() {
        // do not update if not initialized
        if (!initialized) return;
        
        // load the list of nodes
        MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
        mcs.getMapData(mSession.id, new AsyncCallback<MapDataSet>() {
            
            @Override
            public void onSuccess(MapDataSet result) {
                if (result == null) return;
                /**
                 * update nodes
                 */
                if (result.nodes != null) {
                    for (Node n : result.nodes) {
                        MapNode mn = mNodes.get(n.id);
                        
                        if (mn == null) {
                            // create a map node
                            mn = new MapNode(SessionMapWidget.this, n, mMap);
                            mNodes.put(n.id, mn);
                        }
    
                        // set / update nodes position
                        mn.setPosition(n.position, mFix);
                        
                        // show blue marker
                        mn.setIcon(mBlueIcon);
                    }
                }
                
                /**
                 * update links
                 */
                // set of disappeared links
                HashSet<MapLink> disappeared = new HashSet<MapLink>(mLinks);
                
                if (result.links != null) {
                    // remove all active links
                    for (Link l : result.links) {
                        disappeared.remove(l);
                    }
                }
                
                for (MapLink ml : disappeared) {
                    // remove link
                    ml.hide();
                }
                
                // remove all disappeared links
                mLinks.removeAll(disappeared);

                // create MapLink objects for new links
                if (result.links != null) {
                    for (Link l : result.links) {
                        if (mLinks.contains(l)) continue;
                        
                        // create one map link for each link
                        MapNode source = mNodes.get(l.source.id);
                        MapNode target = mNodes.get(l.target.id);
    
                        MapLink ml = new MapLink(source, target);
                        mLinks.add(ml);
                    }
                }
                
                // adjust link position
                for (MapLink ml : mLinks) {
                    MapNode source = mNodes.get(ml.source.id);
                    MapNode target = mNodes.get(ml.target.id);
                    
                    // hide link if source or target is null
                    if ((source == null) || (target == null)) {
                        ml.show(null);
                        continue;
                    }
                    
                    // hide link if source or target has no position
                    if (!source.hasPosition() || !target.hasPosition()) {
                        ml.show(null);
                        continue;
                    }
                    
                    ml.show(mMap);
                }
            }
            
            @Override
            public void onFailure(Throwable caught) {
                // failed to load data
            }
        });
    }

    @Override
    public void onResize(ResizeEvent event) {
        // do not resize if not visible
        if (!isVisible()) {
            mRefreshTimer.cancel();
            return;
        }
        
        Integer width = panelMap.getOffsetWidth();
        Double height = Double.valueOf(width) * Double.valueOf(9.0 / 16.0);
        
        if (width > 0) {
            panelMap.setHeight(height.intValue() + "px");
        }
        
        // enable refresh timer
        mRefreshTimer.scheduleRepeating(1000);
    }
    
    public void onNodeClick(MapNode n) {
        // TODO: show node information
    }
    
    private Timer mRefreshTimer = new Timer() {
        @Override
        public void run() {
            update();
        }
    };

}
