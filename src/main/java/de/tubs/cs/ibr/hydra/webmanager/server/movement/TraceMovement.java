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
import de.tubs.cs.ibr.hydra.webmanager.shared.GeoCoordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.MobilityParameterSet;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public class TraceMovement extends MovementProvider implements Closeable {

    private MobilityParameterSet mParams = null;
    private SessionContainer mContainer = null;
    
    private File mTraceFile = null;
    private BufferedReader mReader = null;
    private boolean mRepeat = false;
    
    private TraceMovement.Entry mLastEntry = null;
    private Queue<Node> mNodePool = new LinkedList<Node>();
    private Map<Long, Node> mNodeMap = new HashMap<Long, Node>();
    
    public TraceMovement(MobilityParameterSet p, SessionContainer sc) {
        mParams = p;
        mContainer = sc;
        
        if (mParams.parameters.containsKey("repeat")) {
            mRepeat = "yes".equals(mParams.parameters.get("repeat"));
        }
        
        if (mParams.parameters.containsKey("tracefile")) {
            mTraceFile = new File(mContainer.getDataPath("data"), mParams.parameters.get("tracefile"));
        }
    }
    
    private static class Header {
        public Long beginTimestamp = 0L;
        public Long endTimestamp = 0L;
        
        public Double areaWidth = 0.0;
        public Double areaHeight = 0.0;
        
        public GeoCoordinates reference = new GeoCoordinates();
        
        private Header() {
        }
        
        public static TraceMovement.Header parse(String data) {
            if (data == null) return null;
            
            // split up data into pieces
            String[] dataset = data.split(" ");
            
            // only decode if there are at least four data values
            if (dataset.length < 8) return null;
            
            TraceMovement.Header header = new TraceMovement.Header();
            
            header.beginTimestamp = Long.valueOf(dataset[0]);
            header.endTimestamp = Long.valueOf(dataset[1]);
            
            header.areaWidth = Double.valueOf(dataset[3]);
            header.areaHeight = Double.valueOf(dataset[5]);
            
            // only decode if there are at least four data values
            if (dataset.length < 10) return header;
            
            Double lat = Double.valueOf(dataset[8]);
            Double lng = Double.valueOf(dataset[9]);
            
            header.reference.setLocation(lat, lng);
            
            return header;
        }
    };
    
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
                // if there is no more data, check repeat variable
                if (mRepeat) {
                    // close trace and start over
                    mReader.close();
                    
                    // reset global variables
                    mLastEntry = null;
                    
                    // reset the elapsed time
                    resetElapsedTime();
                    
                    // open the trace file again
                    openTrace(mTraceFile);
                } else {
                    // we are done
                    throw new MovementFinishedException();
                }
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
        n.position.setLocation(e.position);
        
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
    public void initialize() {
        GeoCoordinates ref = null;
        
        try {
            // open the trace file
            TraceMovement.Header header = TraceMovement.Header.parse( openTrace(mTraceFile) );
            
            // set map reference
            ref = header.reference;
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
        
        for (Node n : getNodes()) {
            if (ref != null) {
                // set map reference coordinates
                n.position.setReference(ref);
            }
            
            // add node to the mapping pool
            mNodePool.offer(n);
        }
    }
    
    private String openTrace(File f) throws FileNotFoundException, IOException {
        InputStream input = null;
        
        if (f.getName().endsWith(".gz")) {
            // open as GZIPStream
            input = new GZIPInputStream(new FileInputStream(f));
        } else {
            // open as standard stream
            input = new FileInputStream(f);
        }
        
        // create file-reader
        mReader = new BufferedReader(new InputStreamReader(input));
        
        // read header line of the trace
        return readLine();
    }
    
    private String readLine() throws IOException {
        String data = mReader.readLine();
        
        if (data == null) return null;
        
        // skip comment lines
        while (data.startsWith("#")) {
            data = mReader.readLine();
            if (data == null) return null;
        }
        
        return data;
    }
}
