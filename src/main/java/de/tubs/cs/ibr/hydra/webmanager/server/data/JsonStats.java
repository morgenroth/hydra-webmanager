package de.tubs.cs.ibr.hydra.webmanager.server.data;

import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
    public static HashMap<Long, String> splitAll(String jsonData) {
        JSONParser parser = new JSONParser();
        HashMap<Long, String> ret = new HashMap<Long, String>();
        
        try {
            // translate JSON to Object
            JSONObject obj = (JSONObject)parser.parse(jsonData);
            
            for (Object id : obj.keySet()) {
                JSONObject data = (JSONObject)obj.get(id);
                ret.put(Long.valueOf((String)id), data.toJSONString());
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return ret;
    }
}
