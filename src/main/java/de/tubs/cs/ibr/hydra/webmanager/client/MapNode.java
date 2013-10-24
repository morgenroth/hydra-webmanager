package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.maps.gwt.client.Circle;
import com.google.maps.gwt.client.CircleOptions;
import com.google.maps.gwt.client.GoogleMap;
import com.google.maps.gwt.client.LatLng;
import com.google.maps.gwt.client.Marker;
import com.google.maps.gwt.client.Marker.ClickHandler;
import com.google.maps.gwt.client.MarkerImage;
import com.google.maps.gwt.client.MouseEvent;

import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;
import de.tubs.cs.ibr.hydra.webmanager.shared.GeoCoordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public class MapNode {
    private Node mNode;
    private Marker mMark;
    private Circle mCircle;
    private CircleOptions mCircleOptions;
    
    private GeoCoordinates mGeo;
    private LatLng mPosition;

    private DataPoint mData;
    private SessionMapWidget mWidget;
    
    private GoogleMap mMap;
    
    private boolean mHide = false;
    private boolean mVisible = false;
    
    public MapNode(SessionMapWidget widget, Node n, GoogleMap map) {
        mWidget = widget;
        mNode = n;
        mMap = map;
        
        mCircleOptions = CircleOptions.create();
        mCircleOptions.setFillOpacity(0.2);
        mCircleOptions.setFillColor("#007bd0");
        mCircleOptions.setStrokeOpacity(0.6);
        mCircleOptions.setStrokeColor("#007bd0");
        mCircleOptions.setStrokeWeight(1);
        mCircleOptions.setZindex(-2);
        
        mCircle = Circle.create(mCircleOptions);
        mCircle.setRadius(mNode.range);
        
        mMark = Marker.create();
        mMark.setTitle(mNode.name);
        mMark.setClickable(true);
        mMark.addClickListener(mClickHandler);
    }
    
    public void setHidden(boolean hidden) {
        mHide = hidden;
        
        boolean showOnMap = (!hidden && hasPosition());
        
        if (!mVisible && showOnMap) {
            if (mCircle != null) mCircle.setMap( mMap );
            if (mMark != null) mMark.setMap( mMap );
            mVisible = true;
        } else if (mVisible  && !showOnMap) {
            if (mCircle != null) mCircle.setMap( null );
            if (mMark != null) mMark.setMap( (GoogleMap) null );
            mVisible = false;
        }
    }

    public void setIcon(MarkerImage icon) {
        mMark.setIcon(icon);
    }
    
    public boolean hasPosition() {
        return mPosition != null;
    }
    
    public void setPosition(Coordinates c, GeoCoordinates fix) {
        // translate coordinates
        setPosition(c == null ? null : c.getGeoCoordinates(fix));
        
        // set node coordinates
        mNode.position = c;
    }

    public void setPosition(GeoCoordinates g) {
        mGeo = g;
        
        if (g == null) {
            mPosition = null;
        } else {
            mPosition = LatLng.create(mGeo.getLat(), mGeo.getLon());
            
            if (mMark != null) {
                // set position
                mMark.setPosition(mPosition);
            }
            
            if (mCircle != null) {
                // set position
                mCircle.setCenter(mPosition);
            }
        }
        
        // decide if hidden or not
        setHidden(mHide);
    }
    
    public LatLng getPosition() {
        return mPosition;
    }
    
    public Node getNode() {
        return mNode;
    }

    public void setNode(Node node) {
        mNode = node;
    }

    public DataPoint getData() {
        return mData;
    }

    public void setData(DataPoint data) {
        mData = data;
    }

    public ClickHandler mClickHandler = new ClickHandler() {
        @Override
        public void handle(MouseEvent event) {
            mWidget.onNodeClick(MapNode.this);
        }
    };
    
    @Override
    public String toString() {
        return mNode.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MapNode) {
            MapNode n = (MapNode)obj;
            return mNode.equals(n.getNode());
        }
        else if (obj instanceof Node) {
            Node n = (Node)obj;
            return mNode.equals(n);
        }
        return super.equals(obj);
    }
}
