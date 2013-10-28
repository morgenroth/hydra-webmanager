package de.tubs.cs.ibr.hydra.webmanager.server.data;

import java.sql.Timestamp;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.DataPoint;

/**
 * Data example
 * {
 * "position": {"y": 0, "x": 0, "state": 0, "z": 0}, 
 * "dtnd": {
 *   "info": {"Neighbors": "0", "Uptime": "38", "Storage-size": "0"}, 
 *   "bundles": {"Received": "0", "Generated": "0", "Stored": "0", "Transmitted": "0", "Aborted": "0", "Requeued": "0", "Expired": "0", "Queued": "0"}
 * }, 
 * "iface": {"lo": {"rx": "0", "tx": "0"}, "eth1": {"rx": "0", "tx": "2846"}, "eth0": {"rx": "7330", "tx": "2784"}}, 
 * "clock": {"Delay": "0.02614", "Timex-tick": "10000", "Timex-offset": "0", "Timex-freq": "0", "Offset": "1.07629"}
 * }
 */

public class JsonStats {
    private static JSONParser mParser = new JSONParser();
    
    private static Double getDouble(Object data) {
        if (data instanceof Double) {
            return (Double)data;
        }
        if (data instanceof Long) {
            return Double.valueOf((Long)data);
        }
        if (data instanceof String) {
            return JsonStats.getDouble(data);
        }
        return 0.0;
    }
    
    private static Long getLong(Object data) {
        if (data instanceof Long) {
            return (Long)data;
        }
        if (data instanceof Double) {
            return ((Double)data).longValue();
        }
        if (data instanceof String) {
            return JsonStats.getLong(data);
        }
        return 0L;
    }
    
    public static HashMap<Long, String> splitAll(String jsonData) {
        HashMap<Long, String> ret = new HashMap<Long, String>();
        
        try {
            // translate JSON to Object
            JSONObject obj = (JSONObject)mParser.parse(jsonData);
            
            for (Object id : obj.keySet()) {
                JSONObject data = (JSONObject)obj.get(id);
                ret.put(Long.valueOf((String)id), data.toJSONString());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
    
    public static HashMap<Long, DataPoint> decodeAll(Timestamp ts, String jsonData) {
        HashMap<Long, DataPoint> ret = new HashMap<Long, DataPoint>();
        
        try {
            // translate JSON to Object
            JSONObject obj = (JSONObject)mParser.parse(jsonData);
            
            for (Object id : obj.keySet()) {
                DataPoint dp = JsonStats.decode(ts, (JSONObject)obj.get(id));
                ret.put(Long.valueOf((String)id), dp);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return ret;
    }

    public static DataPoint decode(Timestamp ts, String jsonData) {
        try {
            // translate JSON to Object
            JSONObject obj = (JSONObject)mParser.parse(jsonData);
            return JsonStats.decode(ts, obj);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static DataPoint decode(Timestamp ts, JSONObject obj) {
        DataPoint ret = new DataPoint();
        
        // assign timestamp
        ret.time = ts;
        
        // return if the json is invalid
        if (obj == null) return ret;
        
        JSONObject position = (JSONObject)obj.get("position");
        if (position != null) {
            double x = JsonStats.getDouble(position.get("x"));
            double y = JsonStats.getDouble(position.get("y"));
            double z = JsonStats.getDouble(position.get("z"));
            ret.coord = new Coordinates(x, y, z);
        }
        
        JSONObject dtnd = (JSONObject)obj.get("dtnd");
        if (dtnd != null) {
            JSONObject dtnd_info = (JSONObject)dtnd.get("info");
            if (dtnd_info != null) {
                ret.dtninfo.neighbors = JsonStats.getLong(dtnd_info.get("Neighbors"));
                ret.dtninfo.uptime = JsonStats.getLong(dtnd_info.get("Uptime"));
                ret.dtninfo.storage_size = JsonStats.getLong(dtnd_info.get("Storage-size"));
            }
            
            JSONObject bundles = (JSONObject)dtnd.get("bundles");
            if (bundles != null) {
                ret.bundlestats.aborted = JsonStats.getLong(bundles.get("Aborted"));
                ret.bundlestats.expired = JsonStats.getLong(bundles.get("Expired"));
                ret.bundlestats.generated = JsonStats.getLong(bundles.get("Generated"));
                ret.bundlestats.queued = JsonStats.getLong(bundles.get("Queued"));
                ret.bundlestats.received = JsonStats.getLong(bundles.get("Received"));
                ret.bundlestats.requeued = JsonStats.getLong(bundles.get("Requeued"));
                ret.bundlestats.transmitted = JsonStats.getLong(bundles.get("Transmitted"));
                ret.bundlestats.stored = JsonStats.getLong(bundles.get("Stored"));
            }
        }
        
        JSONObject clock = (JSONObject)obj.get("clock");
        if (clock != null) {
            ret.clock.delay = JsonStats.getDouble(clock.get("Delay"));
            ret.clock.offset = JsonStats.getDouble(clock.get("Offset"));
            ret.clock.timex_tick = JsonStats.getLong(clock.get("Timex-tick"));
            ret.clock.timex_offset = JsonStats.getLong(clock.get("Timex-offset"));
            ret.clock.timex_freq = JsonStats.getLong(clock.get("Timex-freq"));
        }
        
        JSONObject ifaces = (JSONObject)obj.get("iface");
        
        for (Object key : ifaces.keySet()) {
            JSONObject iface_data = (JSONObject)ifaces.get(key);
            
            DataPoint.InterfaceStats iface = new DataPoint.InterfaceStats();
            iface.name = (String)key;
            
            iface.rx = JsonStats.getLong(iface_data.get("rx"));
            iface.tx = JsonStats.getLong(iface_data.get("tx"));
            
            ret.ifaces.put(iface.name, iface);
        }
        
        return ret;
    }
}
