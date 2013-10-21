package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface MapMarker extends ClientBundle {
    @Source("node-blue.png")
    ImageResource blue();
    
    @Source("node-red.png")
    ImageResource red();
    
    @Source("node-green.png")
    ImageResource green();
}
