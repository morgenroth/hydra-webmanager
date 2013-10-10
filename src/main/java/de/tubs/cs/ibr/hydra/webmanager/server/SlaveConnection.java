package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import de.tubs.cs.ibr.hydra.webmanager.server.data.Database;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;
import de.tubs.cs.ibr.hydra.webmanager.shared.User;

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
    
    public void setSlave(Slave s) {
        mSlave = s;
    }
    
    public Slave doHandshake() throws IOException {
        // receive slave banner
        String data = mReader.readLine();
        
        // read parameters
        String name = null;
        Long capacity = null;
        Long owner = null;
        
        while (!".".equals(data)) {
            data = mReader.readLine();
            
            if (data.contains(": ")) {
                String[] data_pair = data.split(": ");
            
                if ("Identifier".equals(data_pair[0])) {
                    name = data_pair[1];
                }
                else if ("Capacity".equals(data_pair[0])) {
                    capacity = Long.valueOf(data_pair[1]);
                }
                else if ("Owner".equals(data_pair[0])) {
                    String owner_name = data_pair[1];
                    
                    // get username from database
                    User u = Database.getInstance().getUser(owner_name);
                    
                    if (u != null) owner = u.id;
                }
            }
        }
        
        // create a slave object
        mSlave = new Slave(name, owner, capacity);
        
        // set address
        SocketAddress addr = mSocket.getRemoteSocketAddress();
        if (addr instanceof InetSocketAddress) {
            InetSocketAddress iaddr = (InetSocketAddress)addr;
            mSlave.address = iaddr.getHostString();
        } else {
            mSlave.address = addr.toString();
        }
        
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
        }
    }
    
    public void close() throws IOException {
        mRunning = false;
        mSocket.close();
        mReader.close();
    }
}
