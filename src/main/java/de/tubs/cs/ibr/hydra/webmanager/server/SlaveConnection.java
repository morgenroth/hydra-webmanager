package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcasterFactory;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;
import de.tubs.cs.ibr.hydra.webmanager.shared.Event.EventType;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;

public class SlaveConnection extends Thread {
    
    public static ConcurrentHashMap<String, Slave> __slaves__ = new ConcurrentHashMap<String, Slave>();
    
    private Socket mSocket = null;
    private String mIdentifier = null;

    public SlaveConnection(Socket s) {
        this.mSocket = s;
    }

    @Override
    public void run() {
        // create atmosphere broadcast channel
        BroadcasterFactory bf = DefaultBroadcasterFactory.getDefault();
        Broadcaster channel = bf.lookup("events", true);
        
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
            
            // add slave to the global slave list
            __slaves__.put(slave.toString(), slave);
            
            // announce the new slave
            System.out.println("Slave connection: " + slave.toString());
            channel.broadcast(new Event(EventType.SLAVE_CONNECTED));
            
            String data = null;
            while (mSocket.isConnected()) {
                // read next line
                data = reader.readLine();
                
                // if readline returns null the connection is down
                if (data == null) break;
                
                // print out received data
                System.out.println(data);
            }
            
            // remove the slave from active slave list
            __slaves__.remove(slave.toString());
            
            // announce disconnection
            channel.broadcast(new Event(EventType.SLAVE_DISCONNECTED));
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
