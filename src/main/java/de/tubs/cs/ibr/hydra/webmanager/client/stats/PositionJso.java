package de.tubs.cs.ibr.hydra.webmanager.client.stats;

import com.google.gwt.core.client.JavaScriptObject;

public class PositionJso extends JavaScriptObject {
    protected PositionJso() { }

    public final native int getState() /*-{ return this.state; }-*/;
    public final native double getX() /*-{ return this.x; }-*/;
    public final native double getY() /*-{ return this.y; }-*/;
    public final native double getZ() /*-{ return this.z; }-*/;
}
