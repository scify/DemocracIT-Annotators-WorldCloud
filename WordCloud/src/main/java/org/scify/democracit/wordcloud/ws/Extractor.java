package org.scify.democracit.wordcloud.ws;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.scify.democracit.dao.model.Comments;
import org.scify.democracit.demoutils.DataAccess.DBUtils.JSONMessage;
import org.scify.democracit.demoutils.DataAccess.ds.CommentsJPARetriever;
import org.scify.democracit.demoutils.DataAccess.ds.ICommentsRetriever;
import org.scify.democracit.demoutils.logging.BaseEventLogger;
import org.scify.democracit.demoutils.logging.DBAJPAEventLogger;
import org.scify.democracit.wordcloud.dba.IWordCloudDBA;
import org.scify.democracit.wordcloud.impl.IWordCloudExtractor;
import org.scify.democracit.wordcloud.impl.InRAMWordCloudExtractor;
import org.scify.democracit.demoutils.logging.ILogger;
import org.scify.democracit.demoutils.text.HtmlDocumentCleaner;
import org.scify.democracit.wordcloud.dba.JPAWordCloud;
import org.scify.democracit.wordcloud.utils.Configuration;

// example call for test
// http://localhost:8084/WordCloud/Extractor?consultation_id=1&test=true
/**
 * Servlet implementation class Extractor
 */
@WebServlet(name = "Extractor", urlPatterns = {"/Extractor"})
public class Extractor extends HttpServlet {

    private static final long serialVersionUID = 1L;
    /**
     * the current directory
     */
    public String workingDir;
    // the properties file name
    public static final String PROPERTIES = "wordcloud.properties";
    private DataSource dataSource;
    private ILogger logger;

    private EntityManagerFactory emf;
    public static final String PERSISTENCE_RESOURCE = "org.scify_DemoModel_jar_0.1-SNAPSHOTPU";

    @Override
    public void init() throws ServletException {
        // init persistence manager
        emf = Persistence.createEntityManagerFactory(PERSISTENCE_RESOURCE);
//        // debug
//        logger = new BaseEventLogger();
        // init logging
        logger = DBAJPAEventLogger.getInstance(emf);
    }

    @Override
    public void destroy() {
        if (emf != null) {
            emf.close();
        }
    }

    /**
     * @param request the request
     * @param response the response
     * @throws javax.servlet.ServletException
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException {

        response.setContentType("application/json;charset=UTF-8");

        PrintStream out = null;
        int consultation_id;
        int article_id;
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
        // the consultation / article ID to calculate
        int iProcessId = consultation_id == 0 ? article_id : consultation_id;
        Configuration configuration = loadConfig(getServletContext());
        // acquire module name
        String sModulName = configuration.getModuleName();
        // log initiation
        long activity_id = logger.registerActivity(iProcessId, sModulName,
                new JSONMessage(sModulName.concat(" Initializing...")).toJSON());
        // initiate process
        Collection<Comments> cComments = new ArrayList<>();
        IWordCloudDBA storage;
        IWordCloudExtractor extractor;
        ICommentsRetriever comments_retriever;
        try {
            out = new PrintStream(response.getOutputStream());
            // init storage module
//            storage = new PSQLWordCloudDBA(dataSource, configuration, logger);
            storage = new JPAWordCloud(emf, logger);
            // initialize content/segment fetcher module
            comments_retriever = CommentsJPARetriever.getInstance(emf);
            if (consultation_id != 0) {
                cComments = comments_retriever.getCommentsPerConsultationID(consultation_id);
            } else if (article_id != 0) {
                cComments = comments_retriever.getCommentsPerArticleID(article_id);
            }
            cComments = cleanComments(cComments);
            // load extractor. Keep all data in RAM, and store in one DB thread afterwards
            extractor = new InRAMWordCloudExtractor(storage, configuration, logger);
            // generate the term - freq map and store to DB
            extractor.generateWordCloud(cComments, iProcessId, consultation_id != 0);
            // register activity completed
            logger.finalizedActivity(activity_id, iProcessId, sModulName,
                    new JSONMessage(sModulName.concat(" Completed succesfully...")).toJSON());
            // respond
            out.print(new JSONMessage("OK").toJSON());
        } catch (IOException | SQLException ex) {
            // register activity error
            logger.error(activity_id, ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private Collection<Comments> cleanComments(Collection<Comments> cComments) {
        Collection<Comments> res = new ArrayList<>();
        for (Comments comment : cComments) {
            comment = HtmlDocumentCleaner.cleanCommentFromHtml(comment);
            res.add(comment);
        }
        return res;
    }

    public static Configuration loadConfig(ServletContext servletContext) {
        // get configuration from file
        String workingDir = servletContext.getRealPath(DIR_SEP).endsWith(DIR_SEP)
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
}
