package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Link;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public class TraceRecorder {
    private BufferedWriter mOutput = null;
    private File mFile = null;
    private Long mStarted = null;
    
    public TraceRecorder(File file) throws IOException {
        mFile = file;
        mOutput = new BufferedWriter(new FileWriter(mFile));
        mStarted = System.nanoTime();
    }
    
    public void close() throws IOException {
        mOutput.flush();
        mOutput.close();
    }
    
    public void logMovement(Node n, Coordinates position, Double speed, Double heading) throws IOException {
        mOutput.write(getTimestamp() + "\t" + n.id + "\t" + position.getX() + "\t" + position.getY() + "\t" + position.getZ() + "\t" + speed + "\t" + heading);
        mOutput.newLine();
    }
    
    public void logContact(Link link, String action) throws IOException {
        mOutput.write(getTimestamp() + "\t" + link.source.id + "\t" + link.target.id + "\t" + action);
        mOutput.newLine();
    }
    
    private Double getTimestamp() {
        return Double.valueOf(System.nanoTime() - mStarted) / Double.valueOf(TimeUnit.SECONDS.toNanos(1));
    }
}
