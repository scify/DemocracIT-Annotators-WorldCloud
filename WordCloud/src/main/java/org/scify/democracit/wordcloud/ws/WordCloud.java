/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.ws;

import java.io.IOException;
import java.io.PrintWriter;
import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.scify.democracit.demoutils.DataAccess.DBUtils.JSONMessage;
import org.scify.democracit.demoutils.logging.BaseEventLogger;
import org.scify.democracit.demoutils.logging.ILogger;
import org.scify.democracit.wordcloud.dba.IWordCloudDBA;
import org.scify.democracit.wordcloud.dba.PSQLWordCloudDBA;
import org.scify.democracit.wordcloud.response.WordCloudResponse;
import org.scify.democracit.wordcloud.utils.Configuration;

/**
 *
 * @author George K.<gkiom@iit.demokritos.gr>
 */
@WebServlet(name = "WordCloud", urlPatterns = {"/WordCloud"})
public class WordCloud extends HttpServlet {

    private DataSource dataSource;
    private ILogger logger;
    private boolean bInit = false;
    public String workingDir;

    /**
     * Inject resources at the initialization of the WS
     */
    @PostConstruct
    protected void initialize() {
        if (bInit) {
            return;
        }
        // inject datasource connection
        Context initContext;
        try {
            initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup("java:/comp/env");
            dataSource = (DataSource) envContext.lookup("jdbc/word_cloud");
            // log only to output (or file)
            logger = new BaseEventLogger();
            bInit = true;
        } catch (NamingException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
//        response.setContentType("text/html;charset=UTF-8");
        int consultation_id;
        int article_id;
        int n_gram_order = 0;
        String sConsultationID = request.getParameter("consultation_id");
        if (sConsultationID == null) {
            consultation_id = 0;
        } else {
            consultation_id = Integer.parseInt(sConsultationID);
        }
        String sArticleID = request.getParameter("article_id");
        if (sArticleID == null) {
            article_id = 0;
        } else {
            article_id = Integer.parseInt(sArticleID);
        }
        if (consultation_id + article_id == 0) {
            throw new IllegalArgumentException("Provide an Article ID OR a consultation ID please");
        }
        String sNGram = request.getParameter("n_gram_order");
        if (sNGram != null && (sNGram.equals("1") || sNGram.equals("2"))) {
            n_gram_order = Integer.parseInt(sNGram);
        }
        String sMaxTerms = request.getParameter("max_terms");
        // the consultation / article ID to calculate
        int iProcessId = consultation_id == 0 ? article_id : consultation_id;
        // load config
        Configuration conf = loadConfig();
        // acquire module name
        String sModulName = conf.getModuleName().split("\\s+")[0].concat(" responder");
        // register activity
        long activity_id
                = logger.registerActivity(iProcessId, sModulName, new JSONMessage("Wordcloud responds...").toJSON());
        // get term max threshold to respond
        int max_terms = getMaxTermsToResponde(conf, sMaxTerms);
        // query comments and return results as JSON
        PrintWriter out = null;
        IWordCloudDBA storage = null;
        try {
            out = response.getWriter();
            storage = new PSQLWordCloudDBA(dataSource, conf, logger);
            WordCloudResponse res = storage.loadTermCloud(iProcessId, consultation_id != 0, max_terms, n_gram_order);
            // respond
            out.print(res.toJSON());
            // register finalized
            logger.finalizedActivity(activity_id, iProcessId, sModulName);
        } catch (Exception ex) {
            logger.error(activity_id, ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }

//        try (PrintWriter out = response.getWriter()) {
//            /* TODO output your page here. You may use following sample code. */
//            out.println("<!DOCTYPE html>");
//            out.println("<html>");
//            out.println("<head>");
//            out.println("<title>Servlet WordCloud</title>");
//            out.println("</head>");
//            out.println("<body>");
//            out.println("<h1>Servlet WordCloud at " + request.getContextPath() + "</h1>");
//            out.println("</body>");
//            out.println("</html>");
//        }
    }

    private int getMaxTermsToResponde(Configuration conf, String sMaxTerms) {
        // get max terms (from file - default or from parameter)
        int max_terms = conf.getMaxTermsToResponde();
        if (sMaxTerms != null) {
            try {
                int tmp = Integer.parseInt(sMaxTerms);
                if (tmp > 0) {
                    max_terms = tmp;
                }
            } catch (NumberFormatException ex) {
                // pass
            }
        }
        return max_terms;
    }

    private Configuration loadConfig() {
        // get servlet context
        ServletContext servletContext = getServletContext();
        // get configuration from file
        this.workingDir = servletContext.getRealPath("/") + "WEB-INF/";
        // init configuration class
        Configuration configuration = new Configuration(workingDir + Extractor.PROPERTIES);
        // set config working Directory
        configuration.setWorkingDir(workingDir);
        return configuration;
    }
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
