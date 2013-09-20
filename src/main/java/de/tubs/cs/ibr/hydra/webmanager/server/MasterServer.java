package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class MasterServer extends GenericServlet {
    
    private ServerSocket mSockServer = null;
    private Boolean mRunning = true;
    
    private Thread mMasterLoop = new Thread() {

        @Override
        public void run() {
            try {
                mSockServer = new ServerSocket(4244);
                
                while (mRunning) {
                    // accept a new client connection
                    Socket client = mSockServer.accept();
                    
                    // create a new SlaveConnection object
                    SlaveConnection slave = new SlaveConnection(client);
                    
                    // start slave connection in a separate thread
                    slave.start();
                }
            } catch (IOException e) {
                // failed to create a server socket
                e.printStackTrace();
            } finally {
                try {
                    mSockServer.close();
                } catch (IOException e) {
                    // error while closing server socket
                }
            }
        }
    };

    /**
     * Serial ID
     */
    private static final long serialVersionUID = -408760991182258654L;

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
    }

    @Override
    public void init() throws ServletException {
        mMasterLoop.start();
        System.out.println("Master service initialized.");
    }
}
