package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

public class Link implements IsSerializable, Comparable<Node> {
    public Node source;
    public Node target;
    
    public Link() {
        this(null, null);
    }
    
    public Link(Node source, Node target) {
        this.source = source;
        this.target = target;
    }
    
    @Override
    public int compareTo(Node o) {
        if (source.id < o.id) {
            return -1;
        }
        else if (source.id > o.id) {
            return 1;
        }
        
        if (target.id < o.id) {
            return -1;
        }
        else if (target.id > o.id) {
            return 1;
        }
        
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Link) {
            Link other = (Link) obj;
            return (this.source.id == other.source.id) && (this.target.id == other.target.id);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return this.source.id + " -> " + this.target.id;
    }

    @Override
    public int hashCode() {
        return this.source.hashCode() ^ this.target.hashCode();
    }
}
