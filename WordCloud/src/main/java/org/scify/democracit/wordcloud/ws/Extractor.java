package org.scify.democracit.wordcloud.ws;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
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
import org.scify.democracit.model.Comment;
import org.scify.democracit.demoutils.DataAccess.ds.CommentsDSRetriever;
import org.scify.democracit.demoutils.DataAccess.ds.ICommentsDSRetriever;
import org.scify.democracit.wordcloud.dba.IWordCloudDBA;
import org.scify.democracit.wordcloud.dba.PSQLWordCloudDBA;
import org.scify.democracit.wordcloud.impl.IWordCloudExtractor;
import org.scify.democracit.wordcloud.impl.InRAMWordCloudExtractor;
import org.scify.democracit.demoutils.logging.DBAEventLogger;
import org.scify.democracit.demoutils.logging.ILogger;
import org.scify.democracit.demoutils.text.HtmlDocumentCleaner;
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
    private boolean bInit = false;

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
//            logger = new BaseEventLogger();
            logger = new DBAEventLogger(dataSource);
            bInit = true;
        } catch (NamingException ex) {
            ex.printStackTrace();
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
        Configuration configuration = loadConfig();
        // acquire module name
        String sModulName = configuration.getModuleName();
        // log initiation
        long activity_id = logger.registerActivity(iProcessId, sModulName,
                new JSONMessage(sModulName.concat(" Initializing...")).toJSON());
        // initiate process
        Collection<Comment> cComments = new ArrayList<>();
        IWordCloudDBA storage;
        IWordCloudExtractor extractor;
        ICommentsDSRetriever comments_retriever;
        try {
            out = new PrintStream(response.getOutputStream());
            // init storage module
            storage = new PSQLWordCloudDBA(dataSource, configuration, logger);
            // initialize content/segment fetcher module
            comments_retriever = new CommentsDSRetriever(dataSource);
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
        } catch (Exception ex) {
            // register activity error
            logger.error(activity_id, ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private Collection<Comment> cleanComments(Collection<Comment> cComments) {
        Collection<Comment> res = new ArrayList<>();
        for (Comment comment : cComments) {
            comment = HtmlDocumentCleaner.cleanCommentFromHtml(comment);
            res.add(comment);
        }
        return res;
    }

    private Configuration loadConfig() {
        // get servlet context
        ServletContext servletContext = getServletContext();
        // get configuration from file
        this.workingDir = servletContext.getRealPath("/") + "WEB-INF/";
        // init configuration class
        Configuration configuration = new Configuration(workingDir + PROPERTIES);
        // set config working Directory
        configuration.setWorkingDir(workingDir);
        return configuration;
    }
}
