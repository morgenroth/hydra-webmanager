package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.gwt.core.client.JsArray;
import com.google.maps.gwt.client.GoogleMap;
import com.google.maps.gwt.client.LatLng;
import com.google.maps.gwt.client.MVCArray;
import com.google.maps.gwt.client.Polyline;
import com.google.maps.gwt.client.PolylineOptions;

import de.tubs.cs.ibr.hydra.webmanager.shared.Link;

public class MapLink extends Link {
    private Polyline mLine = null;
    private PolylineOptions mOptions = null;
    
    private MapNode mMapTarget;
    private MapNode mMapSource;
    
    public MapLink(MapNode source, MapNode target) {
        super(source.getNode(), target.getNode());
        
        mMapSource = source;
        mMapTarget = target;
    }
    
    public void show(GoogleMap map) {
        if (map == null) return;
        
        MVCArray<LatLng> path = null;
        
        if (mLine == null) {
            mOptions = PolylineOptions.create();
            mOptions.setStrokeOpacity(0.5);
            mOptions.setStrokeColor("#000000");
            mOptions.setStrokeWeight(2);
            mOptions.setZindex(-1);
            
            mLine = Polyline.create(mOptions);
            mLine.setMap(map);
            
            path = JsArray.createArray().cast();
        } else {
            path = mLine.getPath();
            path.clear();
        }
        
        path.push(mMapSource.getPosition());
        path.push(mMapTarget.getPosition());
        
        mLine.setPath(path);
    }
    
    public void hide() {
        if (mLine != null) {
            mLine.setMap(null);
            mLine = null;
        }
    }
    
    public void animate(int position, int max) {
        if (mLine == null) return;
        
        // interpolate movement
        mMapSource.animate(position, max);
        mMapTarget.animate(position, max);
        
        MVCArray<LatLng> path = mLine.getPath();
        path.clear();
    
        path.push(mMapSource.getPosition());
        path.push(mMapTarget.getPosition());
        
        mLine.setPath(path);
    }
    
    public boolean hasSource(MapNode m) {
        return mMapSource.equals(m);
    }
    
    public boolean hasTarget(MapNode m) {
        return mMapTarget.equals(m);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Link) {
            Link l = (Link)obj;
            return (l.source == this.source) && (l.target == this.target);
        }
        return super.equals(obj);
    }
}
