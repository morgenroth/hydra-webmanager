package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.maps.gwt.client.GoogleMap;
import com.google.maps.gwt.client.MapOptions;
import com.google.maps.gwt.client.MapTypeId;
import com.google.maps.gwt.client.MarkerImage;
import com.google.maps.gwt.client.Point;

import de.tubs.cs.ibr.hydra.webmanager.shared.Link;
import de.tubs.cs.ibr.hydra.webmanager.shared.MapDataSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class SessionMapWidget extends Composite implements ResizeHandler {

    private static SessionMapWidgetUiBinder uiBinder = GWT.create(SessionMapWidgetUiBinder.class);

    interface SessionMapWidgetUiBinder extends UiBinder<Widget, SessionMapWidget> {
    }
    
    @UiField SimplePanel panelMap;
    @UiField ListBox listSelectedNode;
    @UiField ListBox listViewType;
    @UiField Button buttonRefresh;

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
    boolean mInitialized = false;
    boolean mInitTriggered = false;
    
    private boolean mVisible = false;
    
    // animation (interpolation) variables
    private int mAnimationRound = 0;
    private int mAnimationMax = 10;
    private final int mAnimationInterval = 100;
    
    // selected node
    private Long selectedNode = null;
    
    // 0 = static, 1 = refresh, 2 = animated
    private int mViewMode = 0;

    public SessionMapWidget() {
        initWidget(uiBinder.createAndBindUi(this));
        
        // add resize handler
        Window.addResizeHandler(this);
        
        // disable list of nodes until all nodes are added
        listSelectedNode.setEnabled(false);
    }
    
    public void onSessionUpdated(Session s) {
        mSession = s;
        
        // adjust resolution according to the session resolution
        mAnimationMax = Double.valueOf(((mSession.resolution == null) ? 1.0 : mSession.resolution) * Double.valueOf(mAnimationMax)).intValue();
    }
    
    @Override
    protected void onDetach() {
        setVisible(false);
        super.onDetach();
    }

    public void initialize(final Session session) {
        // store session globally
        mSession = session;
    }
    
    private void initializeMaps() {
        // create map options
        mOptions = MapOptions.create();
        mOptions.setMapTypeId(MapTypeId.ROADMAP);
        mOptions.setMapTypeControl(true);
        mOptions.setPanControl(true);
        mOptions.setScaleControl(true);
        mOptions.setZoom(14) ;
        mOptions.setDraggable(true);
        mOptions.setScrollwheel(true) ;

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
        mInitialized = true;

        // resize the map frame
        onResize(null);

        // update the data for the first time
        update();
    }
    
    private void animate() {
        // do not update if not initialized
        if (!mInitialized) return;
        
        if (mAnimationRound > 0) {
            // animate all visible nodes
            for (MapNode n : mNodes.values()) {
                n.animate(mAnimationRound, mAnimationMax);
            }
            
            // animate all visible egdes
            for (MapLink l : mLinks) {
                l.animate(mAnimationRound, mAnimationMax);
            }
        } else {
            // update position from the server
            update();
        }
        
        // move to the next animation round
        mAnimationRound = (mAnimationRound + 1) % mAnimationMax;
    }
        
    private void update() {
        // do not update if not initialized
        if (!mInitialized) return;
        
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
                    if (!listSelectedNode.isEnabled()) {
                        Collections.sort(result.nodes);
                    }
                    
                    for (Node n : result.nodes) {
                        MapNode mn = mNodes.get(n.id);
                        
                        if (mn == null) {
                            // create a map node
                            mn = new MapNode(SessionMapWidget.this, n, mMap);
                            mNodes.put(n.id, mn);
                        }
    
                        // set / update nodes position
                        mn.setPosition(n.position.getGeoCoordinates());

                        if (n.id.equals(selectedNode)) {
                            // show red marker if selected
                            mn.setIcon(mRedIcon);
                        } else {
                            // show blue marker
                            mn.setIcon(mBlueIcon);
                        }
                        
                        if (mViewMode == 2) {
                            // animate the first frame
                            mn.animate(mAnimationRound, mAnimationMax);
                        } else {
                            // show most recent position
                            mn.animate(mAnimationMax, mAnimationMax);
                        }
                        
                        // add node to listview
                        if (!listSelectedNode.isEnabled()) {
                            listSelectedNode.addItem(n.name, n.id.toString());
                        }
                    }
                }
                
                if (!listSelectedNode.isEnabled()) {
                    listSelectedNode.setEnabled(true);
                    onNodeSelected(null);
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
                    
                    if (mViewMode == 2) {
                        // animate the first frame
                        ml.animate(mAnimationRound, mAnimationMax);
                    } else {
                        // show most recent position
                        ml.animate(mAnimationMax, mAnimationMax);
                    }
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
        Integer width = panelMap.getOffsetWidth();
        Double height = Double.valueOf(width) * Double.valueOf(9.0 / 16.0);
        
        if (width > 0) {
            panelMap.setHeight(height.intValue() + "px");
        }
    }
    
    public void onNodeClick(MapNode n) {
        // show node information
        listSelectedNode.setSelectedValue(n.getNode().id.toString());
        onNodeSelected(null);
    }
    
    private Timer mRefreshTimer = new Timer() {
        @Override
        public void run() {
            if (mViewMode == 1) {
                update();
            }
            else if (mViewMode == 2) {
                animate();
            }
        }
    };

    public void setVisible(boolean visible) {
        // do resize again
        onResize(null);
        
        // if there is no change, do nothing
        if (mVisible == visible) return;
        
        mVisible = visible;
        
        if (!mInitTriggered && mVisible) {
            mInitTriggered = true;
            
            // load map library
            initializeMaps();
        }

        if ((visible) && (mViewMode == 1)) {
            // enable refresh timer
            Double updateRate = ((mSession.resolution == null) ? 1.0 : mSession.resolution) * 1000.0;
            mRefreshTimer.scheduleRepeating(updateRate.intValue());
        } else if ((visible) && (mViewMode == 2)) {
            // enable refresh timer, every 100ms
            mRefreshTimer.scheduleRepeating(100);
        } else {
            mRefreshTimer.cancel();
        }
        
        if (visible) {
            update();
        }
    }
    
    @UiHandler("buttonRefresh")
    void onRefreshButtonClick(ClickEvent e) {
        update();
    }
    
    @UiHandler("listSelectedNode")
    void onNodeSelected(ChangeEvent e) {
        if (selectedNode != null) {
            mNodes.get(selectedNode).setIcon(mBlueIcon);
        }
        
        selectedNode = Long.valueOf(listSelectedNode.getValue(listSelectedNode.getSelectedIndex()));
        
        if (selectedNode != null) {
            MapNode mn = mNodes.get(selectedNode);
            mn.setIcon(mRedIcon);
            if (mn.getPosition() != null) mMap.panTo(mn.getPosition());
        }
    }
    
    @UiHandler("listViewType")
    void onViewChanged(ChangeEvent e) {
        int newViewMode = listViewType.getSelectedIndex();
        if (mViewMode == newViewMode) return;
        
        if (newViewMode == 0) {
            // static
            mRefreshTimer.cancel();
        }
        else if ((newViewMode == 1) && mVisible) {
            // refresh
            Double updateRate = ((mSession.resolution == null) ? 1.0 : mSession.resolution) * 1000.0;
            mRefreshTimer.scheduleRepeating(updateRate.intValue());
        }
        else if ((newViewMode == 2) && mVisible) {
            // animated
            mRefreshTimer.scheduleRepeating(mAnimationInterval);
        }
        
        mViewMode = newViewMode;
    }
}
