package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import de.tubs.cs.ibr.hydra.webmanager.server.data.SessionContainer;
import de.tubs.cs.ibr.hydra.webmanager.shared.Session;

public class HttpUploadServlet extends HttpServlet {
    
    static final Logger logger = Logger.getLogger(HttpUploadServlet.class.getSimpleName());

    /**
     * serial ID
     */
    private static final long serialVersionUID = 6795024852525835975L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // TODO Auto-generated method stub
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // get session id
        Long sessionId = Long.valueOf(req.getParameter("sid"));
        if (sessionId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        // create session object
        Session session = new Session(sessionId);
        
        // get session container
        SessionContainer sc = SessionContainer.getContainer(session);
        
        // Check that we have a file upload request
        boolean isMultipart = ServletFileUpload.isMultipartContent(req);
        
        if (isMultipart) {
            try {
                // initialize the container
                sc.initialize(null);
                
                // Create a factory for disk-based file items
                FileItemFactory factory = new DiskFileItemFactory(5000000, sc.getDataPath("tmp"));
                
                // Create a new file upload handler
                ServletFileUpload upload = new ServletFileUpload(factory);
    
                // Parse the request
                List<FileItem> items = upload.parseRequest(req);
                
                for (FileItem item : items) {
                    if (item.isFormField()) {
                        // TODO: process form field
                    } else {
                        // logging
                        logger.info("Store file '" + item.getName() + "' (" + item.getFieldName() + ") in session " + sessionId);
                        
                        // process upload
                        File file = new File(sc.getDataPath("data"), item.getName());
                        item.write(file);
                    }
                }
                
                // return completed messages
                resp.setContentType("text/html");
                PrintWriter writer = resp.getWriter();
                writer.println("DONE");
                writer.close();
                
                return;
            } catch (IOException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (FileUploadException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
