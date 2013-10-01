package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SlaveConnection extends Thread {
    
    private Socket mSocket = null;
    private String mIdentifier = null;

    public SlaveConnection(Socket s) {
        this.mSocket = s;
    }

    @Override
    public void run() {
        try {
            // receive slave banner
            BufferedReader reader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            
            // read banner
            String banner = reader.readLine();
            
            // read identifier
            String identifier = reader.readLine();
            mIdentifier = identifier.split(": ")[1];
            
            // create a slave object
            Slave slave = new Slave(mIdentifier);
            
            // set address
            slave.address = mSocket.getRemoteSocketAddress().toString();
            
            // register this slave globally
            MasterServer.register(slave, this);
            
            String data = null;
            while (mSocket.isConnected()) {
                // read next line
                data = reader.readLine();
                
                // if readline returns null the connection is down
                if (data == null) break;
                
                // print out received data
                System.out.println(data);
            }
            
            // unregister this slave
            MasterServer.unregister(slave);
        } catch (IOException e) {
            // error while processing slave connection
            e.printStackTrace();
        } finally {
            try {
                mSocket.close();
            } catch (IOException e) {
                // could not close socket
            }
        }
    }
}
