package de.tubs.cs.ibr.hydra.webmanager.client.stats;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;

public class StatsJso extends JavaScriptObject {
    protected StatsJso() { }
    
    public final native ClockJso getClock() /*-{ return this.clock; }-*/;
    //public final native JsArray<ExtraStatsJso> getExtras() /*-{ return this.collection; }-*/;
    public final native DtndJso getDtnd() /*-{ return this.dtnd; }-*/;
    //public final native JsArray<InterfaceJso> getInterfaces() /*-{ return this.ifaces; }-*/;
    public final native PositionJso getPosition() /*-{ return this.position; }-*/;
    public final native TrafficJso getTraffic() /*-{ return this.traffic; }-*/;
    
    public final static StatsJso create(String json) { return JsonUtils.safeEval(json); };
}
