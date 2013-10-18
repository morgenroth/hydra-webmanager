package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GeoCoordinates implements Comparable<GeoCoordinates>, IsSerializable {
    private Double lat;
    private Double lon;
    
    public GeoCoordinates() {
        this(0.0, 0.0);
    }
    
    public GeoCoordinates(Double lat, Double lon) {
        this.lat = lat;
        this.lon = lon;
    }
    
    public void setLocation(GeoCoordinates c) {
        this.lat = c.lat;
        this.lon = c.lon;
    }
    
    public void setLocation(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }
    
    public double getLat() {
        return lat;
    }
    
    public double getLon() {
        return lon;
    }
    
    @Override
    public String toString() {
        return "[" + lat.toString() + ", " + lon.toString() + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GeoCoordinates) {
            GeoCoordinates other = (GeoCoordinates) obj;
            return (this.lat == other.lat) && (this.lon == other.lon);
        }
        return super.equals(obj);
    }

    @Override
    public int compareTo(GeoCoordinates o) {
        if (this.lat < o.lat) {
            return -1;
        }
        else if (this.lat > o.lat){
            return 1;
        }
        if (this.lon < o.lon) {
            return -1;
        }
        else if (this.lon > o.lon){
            return 1;
        }
        else {
            return 0;
        }
    }

    // simplified algorithm
    public static GeoCoordinates fromCoordinates(Coordinates c, GeoCoordinates fix) {
        // earth radius
        double R = 6378137;

        double dLat = c.getY() / R;
        double dLon = c.getX() / (R * Math.cos(Math.PI * fix.getLat() / 180 ));
        
        double lat = fix.getLat() + dLat * 180 / Math.PI;
        double lon = fix.getLon() + dLon * 180 / Math.PI;

        return new GeoCoordinates(lat, lon);
    }
}
