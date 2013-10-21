package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.gwt.core.client.JsArray;
import com.google.maps.gwt.client.GoogleMap;
import com.google.maps.gwt.client.LatLng;
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
    
    public void activate(GoogleMap map) {
        if (map == null) return;
        
        if (mLine == null) {
            mOptions = PolylineOptions.create();
            mOptions.setStrokeOpacity(1.0);
            mOptions.setStrokeColor("#000000");
            mOptions.setStrokeWeight(2);
            mOptions.setZindex(-1);
            
            mLine = Polyline.create(mOptions);
            mLine.setMap(map);
        }
        
        System.out.println(mMapSource.getPosition().toString());

        JsArray<LatLng> path = JsArray.createArray().cast();
        path.push(mMapSource.getPosition());
        path.push(mMapTarget.getPosition());
        
        mLine.setPath(path);
    }
    
    public void deactivate() {
        if (mLine != null) {
            mLine.setMap(null);
        }
    }
}
