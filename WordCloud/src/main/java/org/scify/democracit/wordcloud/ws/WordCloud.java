/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.ws;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        int consultation_id = 0;
        int article_id = 0;
        int n_gram_order = 0;
        String sMaxTerms = null;
        List<Integer> comment_ids = new ArrayList();
        List<Integer> discussion_thread_ids = new ArrayList();
        // parse parameters
        Enumeration<String> params = request.getParameterNames();
        Map<String, Object> parsed_params = new HashMap();
        while (params.hasMoreElements()) {
            String nextParam = params.nextElement();
            if (nextParam.equalsIgnoreCase(Param.CONSULTATION_ID.getDecl())) {
                consultation_id = Integer.parseInt(request.getParameter(nextParam));
            } else if (nextParam.equalsIgnoreCase(Param.ARTICLE_ID.getDecl())) {
                article_id = Integer.parseInt(request.getParameter(nextParam));
            } else if (nextParam.equalsIgnoreCase(Param.MAX_TERMS.getDecl())) {
                sMaxTerms = request.getParameter(nextParam);
            } else if (nextParam.equalsIgnoreCase(Param.N_GRAM_ORDER.getDecl())) {
                n_gram_order = Integer.parseInt(request.getParameter(nextParam));
                if (n_gram_order != 1 || n_gram_order != 2) {
                    throw new IllegalArgumentException("n_gram_order 1 | 2");
                }
            } else if (nextParam.equalsIgnoreCase(Param.COMMENT_IDS.getDecl())) {
                String insert = request.getParameter(nextParam);
                List<Integer> expList = new Gson().fromJson(insert, List.class);
                comment_ids = expList;
            } else if (nextParam.equalsIgnoreCase(Param.DISCUSSION_THREAD_IDS.getDecl())) {
                String insert = request.getParameter(nextParam);
                List<Integer> expList = new Gson().fromJson(insert, List.class);
                discussion_thread_ids = expList;
            }
        }

        if (consultation_id + article_id == 0) {
            if (comment_ids.isEmpty() && discussion_thread_ids.isEmpty()) {
                throw new IllegalArgumentException("Provide an Article ID OR a consultation ID, OR a list of comment IDs OR a list of discussion thread IDs, please");
            }
        }

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
            WordCloudResponse res = null;
            if (iProcessId > 0) {
                res = storage.loadTermCloud(iProcessId, consultation_id != 0, max_terms, n_gram_order);
            } else {
                boolean comments = false;
                if (!comment_ids.isEmpty()) {
                    comments = true;
                    res = storage.loadTermCloud(comment_ids, comments, max_terms, n_gram_order);
                } else if (!discussion_thread_ids.isEmpty()) {
                    res = storage.loadTermCloud(discussion_thread_ids, comments, max_terms, n_gram_order);
                }
            }
            if (res != null) {
                // respond
                out.print(res.toJSON());
            }
            // register finalized
            logger.finalizedActivity(activity_id, iProcessId, sModulName);
        } catch (Exception ex) {
            logger.error(activity_id, ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
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
        this.workingDir = servletContext.getRealPath(DIR_SEP).endsWith(DIR_SEP)
                // workaround for tomcat 8
                ? servletContext.getRealPath(DIR_SEP).concat("WEB-INF").concat(DIR_SEP)
                : servletContext.getRealPath(DIR_SEP).concat(DIR_SEP).concat("WEB-INF").concat(DIR_SEP);
        // init configuration class
        Configuration configuration = new Configuration(workingDir + Extractor.PROPERTIES);
        // set config working Directory
        configuration.setWorkingDir(workingDir);
        return configuration;
    }

    public static final String DIR_SEP = System.getProperty("file.separator");

    public enum Param {

        CONSULTATION_ID("consultation_id"),
        ARTICLE_ID("article_id"),
        N_GRAM_ORDER("n_gram_order"),
        MAX_TERMS("max_terms"),
        DISCUSSION_THREAD_IDS("discussion_thread_ids"),
        COMMENT_IDS("comment_ids");

        private String param;

        private Param(String param) {
            this.param = param;
        }

        public String getDecl() {
            return param;
        }
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
