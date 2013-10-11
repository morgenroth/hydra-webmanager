package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.SynchronousQueue;

import de.tubs.cs.ibr.hydra.webmanager.server.data.Database;
import de.tubs.cs.ibr.hydra.webmanager.shared.Node;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;
import de.tubs.cs.ibr.hydra.webmanager.shared.Slave;
import de.tubs.cs.ibr.hydra.webmanager.shared.User;

public class SlaveConnection extends Thread {
    
    private Socket mSocket = null;
    private BufferedReader mReader = null;
    private BufferedWriter mWriter = null;
    private Slave mSlave = null;
    private boolean mRunning = true;
    
    // create a queue for command responses
    private SynchronousQueue<String> mResponseQueue = new SynchronousQueue<String>(); 

    public SlaveConnection(Socket s) throws IOException {
        this.mSocket = s;
        
        // open buffered
        mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        mWriter = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
    }
    
    public String getIdentifier() {
        if (mSlave == null) return null;
        return mSlave.name;
    }
    
    public void setSlave(Slave s) {
        mSlave = s;
    }
    
    public synchronized Slave doHandshake() throws IOException {
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
                
                // put the 'response' into the queue
                mResponseQueue.put(data);
            }
        } catch (IOException e) {
            // error while processing slave connection
            e.printStackTrace();
        } catch (InterruptedException e) {
            // interrupted while processing slave connection
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
    
    public synchronized void close() throws IOException {
        // send 'quit'
        mWriter.write("quit\n");
        mWriter.flush();
        
        try {
            // wait for a response
            if (receiveResponse() != 200) {
                // something went wrong!
            }
            
            mRunning = false;
            mSocket.close();
            mReader.close();
            mWriter.close();
        } catch (InterruptedException e) {
            // interrupted
        }
    }
    
    public class SessionRunTimeoutException extends Exception {

        /**
         * serial ID
         */
        private static final long serialVersionUID = 965399090241452637L;
        
    };
    
    public class SessionNotFoundException extends Exception {

        /**
         * serial ID
         */
        private static final long serialVersionUID = 3003209709850009366L;
        
    };
    
    private synchronized Long receiveResponse() throws InterruptedException {
        String data = mResponseQueue.take();
        
        String[] data_pair = data.split(" ", 2);
        
        Long code = Long.valueOf(data_pair[0]);
        String message = data_pair[1];
        
        System.out.println(message);
        
        return code;
    }
    
    public synchronized void createSession(Session s, String url) throws IOException, InterruptedException {
        // session create <session-id> <hydra-url>
        mWriter.write("session create " + s.id.toString() + " " + url +"\n");
        mWriter.flush();
        
        // wait for a response
        if (receiveResponse() != 200) {
            // something went wrong!
        }
    }
    
    public synchronized void destroySession(Session s) throws IOException, SessionNotFoundException, InterruptedException {
        // session destroy <session-id>
        mWriter.write("session destroy " + s.id.toString() + "\n");
        mWriter.flush();
        
        // wait for a response
        if (receiveResponse() == 401) {
            // session not found
            throw new SessionNotFoundException();
        }
    }
    
    public synchronized void prepareSession(Session s) throws IOException, SessionNotFoundException, InterruptedException {
        // session prepare <session-id>
        mWriter.write("session prepare " + s.id.toString() + "\n");
        mWriter.flush();
        
        // wait for a response
        if (receiveResponse() == 401) {
            // session not found
            throw new SessionNotFoundException();
        }
    }
    
    public synchronized void runSession(Session s) throws IOException, SessionNotFoundException, SessionRunTimeoutException, InterruptedException {
        // session run <session-id>
        mWriter.write("session run " + s.id.toString() + "\n");
        mWriter.flush();
        
        // wait for a response
        Long code = receiveResponse();
        
        if (code == 401) {
            // session not found
            throw new SessionNotFoundException();
        }
        else if (code == 300) {
            // timed out
            throw new SessionRunTimeoutException();
        }
    }
    
    public synchronized void stopSession(Session s) throws IOException, SessionNotFoundException, InterruptedException {
        // session stop <session-id>
        mWriter.write("session stop " + s.id.toString() + "\n");
        mWriter.flush();
        
        // wait for a response
        if (receiveResponse() == 401) {
            // session not found
            throw new SessionNotFoundException();
        }
    }
    
    public synchronized void createNode(Node n) throws IOException, SessionNotFoundException, InterruptedException {
        // node create <session-id> <node-id> <ip-address> <node-name>
        mWriter.write("node create " + n.sessionId.toString() + " " + n.id.toString() + " " + n.address + " " + n.name + "\n");
        mWriter.flush();
        
        // wait for a response
        if (receiveResponse() == 401) {
            // session not found
            throw new SessionNotFoundException();
        }
    }
    
    public synchronized void destroyNode(Node n) throws IOException, SessionNotFoundException, InterruptedException {
        // node destroy <session-id> <node-id>
        mWriter.write("node create " + n.sessionId.toString() + " " + n.id.toString() + "\n");
        mWriter.flush();
        
        // wait for a response
        if (receiveResponse() == 401) {
            // session not found
            throw new SessionNotFoundException();
        }
    }
    
    public ArrayList<Node> getNodes(Session s) throws IOException, SessionNotFoundException, InterruptedException {
        ArrayList<Node> ret = new ArrayList<Node>();
        String data = doAction(s.id, "list nodes");
        
        if (data != null) {
            Database db = Database.getInstance();
            
            for (String l : data.split("\n")) {
                Long node_id = Long.valueOf(l.trim().substring(1));
                Node n = db.getNode(node_id);
                if (n != null) {
                    ret.add(n);
                }
            }
        }
        
        return ret;
    }
    
    public String execute(Node n, String command) throws IOException, SessionNotFoundException, InterruptedException {
        return doAction(n.sessionId, "script " + n.id.toString() + " " + command);
    }
    
    public void setClock(Node n, Long offset, Long frequency, Long sec, Long usec) throws IOException, SessionNotFoundException, InterruptedException {
        String action = "clock " + n.id.toString() + " ";
        
        action += (offset == null) ? "*" : offset.toString();
        action += " ";
        
        action += (frequency == null) ? "*" : frequency.toString();
        action += " ";
        
        action += (sec == null) ? "*" : sec.toString();
        action += " ";
        
        action += (usec == null) ? "*" : usec.toString();
        
        doAction(n.sessionId, action);
    }
    
    public void setPosition(Node n, Double x, Double y, Double z) throws IOException, SessionNotFoundException, InterruptedException {
        String action = "position " + n.id.toString() + " ";
        
        action += (x == null) ? "0.0" : x.toString();
        action += " ";
        
        action += (y == null) ? "0.0" : y.toString();
        action += " ";
        
        action += (z == null) ? "0.0" : z.toString();
        
        doAction(n.sessionId, action);
    }
    
    public String getStats(Node n) throws IOException, SessionNotFoundException, InterruptedException {
        return doAction(n.sessionId, "stats " + n.id.toString());
    }
    
    public String executeDtndCommand(Node n, String command) throws IOException, SessionNotFoundException, InterruptedException {
        return doAction(n.sessionId, "dtnd " + n.id.toString() + " " + command);
    }
    
    public void connectionUp(Node n, Node r) throws IOException, SessionNotFoundException, InterruptedException {
        // split address into netmask and ip-address
        String[] addr = r.address.split("/");
        
        doAction(n.sessionId, "up " + n.id.toString() + " " + addr[0]);
    }
    
    public void connectionDown(Node n, Node r) throws IOException, SessionNotFoundException, InterruptedException {
        // split address into netmask and ip-address
        String[] addr = r.address.split("/");
        
        doAction(n.sessionId, "down " + n.id.toString() + " " + addr[0]);
    }
    
    /**
     * Execute an action on the node
     * @param n
     * @param action
     * @return
     * @throws IOException
     * @throws SessionNotFoundException
     * 
     * list nodes
     * 
     * script <node-id> <command>
     * 
     * clock <node-id> <offset> <frequency> <sec> <usec>
     * 
     * position <node-id> <x> <y> <z>
     * 
     * stats <node-id>
     * 
     * dtnd <node-id> <action>
     * 
     * up <node-id> <ip-address>
     * 
     * down <node-id> <ip-address>
     * @throws InterruptedException 
     */
    private synchronized String doAction(Long session_id, String action) throws IOException, SessionNotFoundException, InterruptedException {
        // action <session-id> <action-to-execute>
        mWriter.write("action " + session_id.toString() + " " + action + "\n");
        mWriter.flush();
        
        // wait for a response
        Long code = receiveResponse();
        
        if (code == 401) {
            // session not found
            throw new SessionNotFoundException();
        }
        else if (code == 212) {
            String ret = "";
            String data = mResponseQueue.take();
            while (!".".equals(data)) {
                ret += data;
                data = mResponseQueue.take();
            }
            return ret;
        }
        
        return null;
    }
}
