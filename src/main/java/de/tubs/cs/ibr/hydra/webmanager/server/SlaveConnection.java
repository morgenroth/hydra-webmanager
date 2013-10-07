package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SlaveConnection extends Thread {
    
    private Socket mSocket = null;
    private BufferedReader mReader = null;
    private Slave mSlave = null;
    private boolean mRunning = true;

    public SlaveConnection(Socket s) throws IOException {
        this.mSocket = s;
        
        // open buffered
        mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
    }
    
    public String getIdentifier() {
        if (mSlave == null) return null;
        return mSlave.name;
    }
    
    public Slave doHandshake() throws IOException {
        // receive slave banner
        String banner = mReader.readLine();
        
        // read identifier
        String identifier = mReader.readLine();
        String name = identifier.split(": ")[1];
        
        // create a slave object
        mSlave = new Slave(name);
        
        // set address
        mSlave.address = mSocket.getRemoteSocketAddress().toString();
        
        return mSlave;
    }

    @Override
    public void run() {
        try {
            String data = null;
            while (mRunning) {
                // read next line
                data = mReader.readLine();
                
                // if readline returns null the connection is down
                if (data == null) break;
                
                // print out received data
                System.out.println(data);
            }
        } catch (IOException e) {
            // error while processing slave connection
            e.printStackTrace();
        } finally {
            // unregister this slave
            MasterServer.unregister(mSlave);
            
            try {
                mSocket.close();
            } catch (IOException e) {
                // could not close socket
            }
            
            // mark this thread as detached
            setDaemon(true);
        }
    }
    
    public void close() throws IOException {
        mRunning = false;
        mSocket.close();
        mReader.close();
    }
}
