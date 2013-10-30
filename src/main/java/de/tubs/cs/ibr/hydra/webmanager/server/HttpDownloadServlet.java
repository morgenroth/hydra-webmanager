package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.tubs.cs.ibr.hydra.webmanager.server.data.Database;
import de.tubs.cs.ibr.hydra.webmanager.server.data.SessionContainer;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class HttpDownloadServlet extends HttpServlet {
    
    static final Logger logger = Logger.getLogger(HttpDownloadServlet.class.getSimpleName());

    /**
     * serial ID
     */
    private static final long serialVersionUID = 1501289292088855980L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = req.getParameter("type");
        
        if ("stats".equals(type)) {
            // get session id
            Long sessionId = Long.valueOf(req.getParameter("session"));
            if (sessionId == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // create session object
            Session session = new Session(sessionId);

            // logging
            logger.fine("Stats dump for session " + session.toString() + " requested");
            
            // get output stream
            OutputStream out = resp.getOutputStream();
            
            // set content type
            resp.setContentType("application/force-download");
            
            // set encoding
            resp.setCharacterEncoding("binary");
            resp.setHeader("Content-Transfer-Encoding", "binary");
            
            // set download filename
            resp.setHeader("Content-Disposition","attachment; filename=hydra-" + sessionId.toString() + ".dump");
            
            // dump stats into the output stream
            Database.getInstance().dumpStats(session, out);
            
            // close the stream
            out.close();
        }
        else if ("trace".equals(type)) {
            // get session id
            Long sessionId = Long.valueOf(req.getParameter("session"));
            if (sessionId == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // create session object
            Session session = new Session(sessionId);
            
            // get filename parameter
            String filename = req.getParameter("filename");
            if (filename == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // logging
            logger.fine("Trace '" + filename + "' of session " + sessionId + " requested");
            
            // get session container
            SessionContainer sc = SessionContainer.getContainer(session);
            
            try {
                // initialize the container
                sc.initialize(null);
                
                // create file object of the trace file
                File trace = new File(sc.getTracePath(), filename);
                
                if (!trace.exists()) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                
                // set content type
                resp.setContentType("application/force-download");
                
                // set encoding
                resp.setCharacterEncoding("binary");
                resp.setHeader("Content-Transfer-Encoding", "binary");
                
                // set content length
                resp.setContentLength(((Long)trace.length()).intValue());
                
                // set download filename
                resp.setHeader("Content-Disposition","attachment; filename=hydra-" + sessionId.toString() + "-" + filename);
                
                // get output stream
                OutputStream out = resp.getOutputStream();
                
                // get the trace file
                dumpFile(trace, out);
                
                // close the stream
                out.close();
            } catch (IOException e) {
                // copying failed
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
    
    private static void dumpFile(File dumpfile, OutputStream output) throws FileNotFoundException, IOException {
        try (InputStream in = new FileInputStream(dumpfile)) {
            byte[] buffer = new byte[4096];
            
            int length = 0;
            while ((length = in.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        }
    }
}