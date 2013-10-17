package de.tubs.cs.ibr.hydra.webmanager.server.movement;

import java.util.HashSet;

import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Link;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public class ContactProvider implements MovementProvider.MovementHandler {
    public interface ContactHandler {
        public void onContact(Link link);
        public void onSeparation(Link link);
    }
    
    private HashSet<Node> mNodes = new HashSet<Node>();
    private HashSet<ContactHandler> mListener = new HashSet<ContactHandler>();
    private HashSet<Link> mLinkSet = new HashSet<Link>();

    protected HashSet<Node> getNodes() {
        return mNodes;
    }
    
    protected void fireOnContactEvent(Link link) {
        synchronized(mListener) {
            for (ContactHandler h : mListener) {
                h.onContact(link);
            }
        }
    }
    
    protected void fireOnSeparationEvent(Link link) {
        synchronized(mListener) {
            for (ContactHandler h : mListener) {
                h.onSeparation(link);
            }
        }
    }
    
    public void addContactHandler(ContactHandler c) {
        mListener.add(c);
    }
    
    public void removeContactHandler(ContactHandler c) {
        mListener.remove(c);
    }
    
    /**
     * Add a node to observe
     * @param n
     */
    public void add(Node n) {
        mNodes.add(n);
    }
    
    /**
     * Removes a node
     * @param n
     */
    public void remove(Node n) {
        mNodes.remove(n);
    }

    @Override
    public void onMovement(Node n, Coordinates position, Double speed, Double heading) {
        // no position, no action!
        if (n.position == null) return;
        
        // check distance for all nodes
        for (Node other : getNodes()) {
            // no position, no action!
            if (other.position == null) continue;
            
            // check distance to the other node
            Double distance = n.position.distance(other.position);
            
            Link l = new Link(n, other);
            
            if ((n.range > 0.0) && (distance >= n.range)) {
                // contact
                if (mLinkSet.add(l)) {
                    fireOnContactEvent(l);
                }
            } else {
                // separation
                if (mLinkSet.remove(l)) {
                    fireOnSeparationEvent(l);
                }
            }
        }
    }
}
