package de.tubs.cs.ibr.hydra.webmanager.server.movement;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.zip.GZIPInputStream;

import de.tubs.cs.ibr.hydra.webmanager.server.data.SessionContainer;
import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public class TraceMovement extends MovementProvider implements Closeable {

    private MobilityParameterSet mParams = null;
    private SessionContainer mContainer = null;
    
    private File mTraceFile = null;
    private BufferedReader mReader = null;
    
    private TraceMovement.Entry mLastEntry = null;
    private Queue<Node> mNodePool = new LinkedList<Node>();
    private Map<Long, Node> mNodeMap = new HashMap<Long, Node>();
    
    public TraceMovement(MobilityParameterSet p, SessionContainer sc) {
        mParams = p;
        mContainer = sc;
        
        if (mParams.parameters.containsKey("tracefile")) {
            mTraceFile = new File(mContainer.getDataPath("data"), mParams.parameters.get("tracefile"));
            
            try {
                InputStream input = null;
                
                if (mTraceFile.getName().endsWith(".gz")) {
                    // open as GZIPStream
                    input = new GZIPInputStream(new FileInputStream(mTraceFile));
                } else {
                    // open as standard stream
                    input = new FileInputStream(mTraceFile);
                }
                
                // create file-reader
                mReader = new BufferedReader(new InputStreamReader(input));

                // read header line of the trace
                String header = mReader.readLine();
                
                // TODO: process header
            } catch (FileNotFoundException e) {
                // error
                e.printStackTrace();
                
                // reset the reader object
                mReader = null;
            } catch (IOException e) {
                // error
                e.printStackTrace();
                
                // reset the reader object
                mReader = null;
            }
        }
    }
    
    private static class Entry {
        public Double timestamp = null;
        public Long node = null;
        public Coordinates position = null;
        
        private Entry() {
        }
        
        public static TraceMovement.Entry parse(String data) {
            if (data == null) return null;
            
            // split up data into pieces
            String[] dataset = data.split(" ");
            
            // only decode if there are at least four data values
            if (dataset.length < 4) return null;
            
            TraceMovement.Entry e = new TraceMovement.Entry();
            
            e.timestamp = Double.valueOf(dataset[0]);
            e.node = Long.valueOf(dataset[1]);
            
            Double x = Double.valueOf(dataset[2]);
            Double y = Double.valueOf(dataset[3]);
            
            // negative positions set a node invisible
            if ((x < 0.0) || (y < 0.0)) {
                e.position = null;
            } else {
                e.position = new Coordinates(x, y);
            }
            
            return e;
        }

        @Override
        public String toString() {
            return timestamp + " " + node + " " + position;
        }
    }

    @Override
    public void update() throws MovementFinishedException {
        // stop session if the reader is not initialized
        if (mReader == null) throw new MovementFinishedException();
        
        // get the elapsed time
        Double elapsedTime = getElapsedTime();

        // do not read more entries if the last entry is still in the 'future'
        if ((mLastEntry != null) && (mLastEntry.timestamp > elapsedTime)) return;
        
        // process last entry if present
        if (mLastEntry != null) {
            process(mLastEntry);
        }
        
        try {
            String data = null;
            
            // read input file by line
            while ((data = mReader.readLine()) != null) {
                // parse movement data
                mLastEntry = TraceMovement.Entry.parse(data);
                
                // do not process move items if they are in the 'future'
                if (mLastEntry.timestamp > elapsedTime) return;
                
                // process the current item
                process(mLastEntry);
            }
            
            if (data == null) {
                // if there is no more data, we are done
                throw new MovementFinishedException();
            }
        } catch (IOException e) {
            // error
            e.printStackTrace();
            
            // stop session on error
            throw new MovementFinishedException();
        }
    }
    
    private void process(TraceMovement.Entry e) {
        Node n = null;
        
        System.out.println("Processing " + e);
        
        if (mNodeMap.containsKey(e.node)) {
            n = mNodeMap.get(e.node);
        } else {
            try {
                // get a not mapped node
                n = mNodePool.remove();
                
                // add mapping for new nodes
                mNodeMap.put(e.node, n);
            } catch (NoSuchElementException ex) {
                // map this node to 'null' (do not process)
                mNodeMap.put(e.node, null);
            }
        }
        
        // do not process this node if it is mapped to 'null'
        if (n == null) return;
        
        // assign new node position
        n.position = e.position;
        
        // fire moved event
        fireOnMovementEvent(n, n.position, n.speed, n.heading);
    }

    @Override
    public void close() throws IOException {
        if (mReader != null) {
            mReader.close();
        }
    }

    @Override
    public void add(Node n) {
        // add node to the super-class
        super.add(n);
        
        // add node to the mapping pool
        mNodePool.offer(n);
    }
}
