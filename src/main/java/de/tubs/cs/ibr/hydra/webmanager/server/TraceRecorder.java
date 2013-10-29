package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import de.tubs.cs.ibr.hydra.webmanager.shared.Coordinates;
import de.tubs.cs.ibr.hydra.webmanager.shared.Link;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;

public class TraceRecorder {
    private OutputStream mOutput = null;
    private File mFile = null;
    private Long mStarted = null;
    
    public TraceRecorder(File file) throws IOException {
        mFile = file;
        mOutput = new GZIPOutputStream(new FileOutputStream(mFile));
        mStarted = System.nanoTime();
    }
    
    public void close() throws IOException {
        mOutput.flush();
        mOutput.close();
    }
    
    public void logMovement(Node n, Coordinates position, Double speed, Double heading) throws IOException {
        String record = getTimestamp() + "\t" + n.id + "\t" + position.getX() + "\t" + position.getY() + "\t" + position.getZ() + "\t" + speed + "\t" + heading + "\n";
        mOutput.write(record.getBytes());
    }
    
    public void logContact(Link link, String action) throws IOException {
        String record = getTimestamp() + "\t" + link.source.id + "\t" + link.target.id + "\t" + action + "\n";
        mOutput.write(record.getBytes());
    }
    
    private Double getTimestamp() {
        return Double.valueOf(System.nanoTime() - mStarted) / Double.valueOf(TimeUnit.SECONDS.toNanos(1));
    }
}
