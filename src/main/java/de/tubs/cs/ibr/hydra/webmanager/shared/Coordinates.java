package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Coordinates implements Comparable<Coordinates>, IsSerializable {
    private Double x;
    private Double y;
    private Double z;
    
    private GeoCoordinates georeference = null;
    
    public Coordinates() {
        this(null, null, null);
    }
    
    public Coordinates(Double x, Double y) {
        this(x, y, null);
    }
    
    public Coordinates(Double x, Double y, Double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void setLocation(Coordinates c) {
        if (c == null) {
            this.x = null;
            this.y = null;
            this.z = null;
            return;
        }
        this.x = c.x;
        this.y = c.y;
        this.z = c.z;
    }
    
    public void setLocation(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public void move(double dx, double dy) {
        if (isInvalid()) return;
        this.x += dx;
        this.y += dy;
    }
    
    public void move(double dx, double dy, double dz) {
        if (isInvalid()) return;
        this.x += dx;
        this.y += dy;
        this.z += dz;
    }
    
    public void flipX() {
        if (isInvalid()) return;
        this.x *= -1.0;
    }
    
    public void flipY() {
        if (isInvalid()) return;
        this.y *= -1.0;
    }
    
    public double distance(Coordinates other) {
        if (isInvalid()) return 0.0;
        
        double dx = this.x - other.x;
        double dy = this.y - other.y;

        if (this.z == null) {
            return Math.sqrt(dx*dx + dy*dy);
        }
        
        double dz = this.z - other.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
    
    public double getX() {
        if (x == null) return 0.0;
        return x;
    }
    
    public double getY() {
        if (y == null) return 0.0;
        return y;
    }
    
    public double getZ() {
        if (z == null) return 0.0;
        return z;
    }
    
    public void setReference(GeoCoordinates ref) {
        georeference = ref;
    }
    
    public GeoCoordinates getGeoCoordinates() {
        if (georeference == null) return null;
        return GeoCoordinates.fromCoordinates(this, georeference);
    }
    
    public GeoCoordinates getGeoCoordinates(GeoCoordinates fix) {
        return GeoCoordinates.fromCoordinates(this, fix);
    }
    
    @Override
    public String toString() {
        if (isInvalid()) return "(invalid)";
        
        if (z == null)
            return "(" + x.toString() + ", " + y.toString() + ")";
        else
            return "(" + x.toString() + ", " + y.toString() + ", " + z.toString() + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Coordinates) {
            Coordinates other = (Coordinates) obj;
            return (this.x == other.x) && (this.y == other.y) && (this.z == other.z);
        }
        return super.equals(obj);
    }

    @Override
    public int compareTo(Coordinates o) {
        if (this.y < o.y) {
            return -1;
        }
        else if (this.y > o.y){
            return 1;
        }
        if (this.x < o.x) {
            return -1;
        }
        else if (this.x > o.x){
            return 1;
        }
        if (this.z < o.z) {
            return -1;
        }
        else if (this.z > o.z){
            return 1;
        }
        else {
            return 0;
        }
    }
    
    public boolean isInvalid() {
        return (this.x == null) || (this.y == null);
    }
}
