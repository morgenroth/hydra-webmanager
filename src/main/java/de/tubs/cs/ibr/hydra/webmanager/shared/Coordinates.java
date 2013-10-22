package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Coordinates implements Comparable<Coordinates>, IsSerializable {
    private Double x;
    private Double y;
    private Double z;
    
    public Coordinates() {
        this(0.0, 0.0, null);
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
        this.x += dx;
        this.y += dy;
    }
    
    public void move(double dx, double dy, double dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
    }
    
    public void flipX() {
        this.x += -1;
    }
    
    public void flipY() {
        this.y += -1;
    }
    
    public double distance(Coordinates other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;

        if (this.z == null) {
            return Math.sqrt(dx*dx + dy*dy);
        }
        
        double dz = this.z - other.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public GeoCoordinates getGeoCoordinates(GeoCoordinates fix) {
        return GeoCoordinates.fromCoordinates(this, fix);
    }
    
    public double getZ() {
        if (z == null) return 0.0;
        return z;
    }
    
    @Override
    public String toString() {
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
}
