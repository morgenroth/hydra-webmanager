package de.tubs.cs.ibr.hydra.webmanager.client.stats;

import com.google.gwt.core.client.JavaScriptObject;

public class TrafficJso extends JavaScriptObject {
    protected TrafficJso() { }

    public final native int getInTcpByte() /*-{ return this["in"].tcp_byte; }-*/;
    public final native int getInTcpPkt() /*-{ return this["in"].tcp_pkt; }-*/;
    public final native int getOutTcpByte() /*-{ return this["out"].tcp_byte; }-*/;
    public final native int getOutTcpPkt() /*-{ return this["out"].tcp_pkt; }-*/;
    
    public final native int getInUdpByte() /*-{ return this["in"].udp_byte; }-*/;
    public final native int getInUdpPkt() /*-{ return this["in"].udp_pkt; }-*/;
    public final native int getOutUdpByte() /*-{ return this["out"].udp_byte; }-*/;
    public final native int getOutUdpPkt() /*-{ return this["out"].udp_pkt; }-*/;
    
    public final native int getInIcmpByte() /*-{ return this["in"].icmp_byte; }-*/;
    public final native int getInIcmpPkt() /*-{ return this["in"].icmp_pkt; }-*/;
    public final native int getOutIcmpByte() /*-{ return this["out"].icmp_byte; }-*/;
    public final native int getOutIcmpPkt() /*-{ return this["out"].icmp_pkt; }-*/;
}
