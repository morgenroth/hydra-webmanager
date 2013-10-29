package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.IOException;
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
        // set encoding for the response
        final String encoding = req.getCharacterEncoding();
        resp.setCharacterEncoding(encoding);
        
        String type = req.getParameter("type");

        if ("stats".equals(type)) {
            Long sessionId = Long.valueOf(req.getParameter("session"));
            Session session = new Session(sessionId);

            logger.fine("Stats dump for session " + session.toString() + " requested");
            
            // get output stream
            OutputStream out = resp.getOutputStream();
            
            // set content type
            resp.setContentType("text/plain");
            
            // set download filename
            resp.setHeader("Content-Disposition","inline;filename=hydra-" + sessionId.toString() + ".dump");
            
            // dump stats into the output stream
            Database.getInstance().dumpStats(session, out);
            
            // close the stream
            out.close();
        }
        else if ("trace".equals(type)) {
            Long sessionId = Long.valueOf(req.getParameter("session"));
            Session session = new Session(sessionId);
            
            String filename = req.getParameter("filename");
            
            logger.fine("Trace '" + filename + "' of session " + sessionId + " requested");
            
            SessionContainer sc = SessionContainer.getContainer(session);
            
            try {
                // initialize the container
                sc.initialize(null);
                
                // set content type
                resp.setContentType("text/plain");
                
                // set content length
                resp.setContentLength(((Long)sc.getTraceSize(filename)).intValue());
                
                // set download filename
                resp.setHeader("Content-Disposition","inline;filename=hydra-" + sessionId.toString() + "-" + filename);
                
                // get output stream
                OutputStream out = resp.getOutputStream();
                
                // get the trace file
                sc.dumpTrace(filename, out);
                
                // close the stream
                out.close();
            } catch (IOException e) {
                // copying failed
            }
        }
    }
}