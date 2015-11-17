package org.scify.democracit.wordcloud.ws;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.scify.democracit.dao.jpacontroller.CommentTermJpaController;
import org.scify.democracit.dao.jpacontroller.ConsultationJpaController;
import org.scify.democracit.dao.jpacontroller.exceptions.NonexistentEntityException;
import org.scify.democracit.dao.model.CommentTerm;
import org.scify.democracit.dao.model.Comments;
import org.scify.democracit.dao.model.Consultation;
import org.scify.democracit.demoutils.DataAccess.DBUtils.JSONMessage;
import org.scify.democracit.demoutils.DataAccess.ds.CommentsJPARetriever;
import org.scify.democracit.demoutils.DataAccess.ds.ICommentsRetriever;
import org.scify.democracit.demoutils.logging.BaseEventLogger;
import org.scify.democracit.wordcloud.dba.IWordCloudDBA;
import org.scify.democracit.wordcloud.impl.IWordCloudExtractor;
import org.scify.democracit.wordcloud.impl.InRAMWordCloudExtractor;
import org.scify.democracit.demoutils.logging.ILogger;
import org.scify.democracit.demoutils.text.HtmlDocumentCleaner;
import org.scify.democracit.wordcloud.dba.JPAWordCloud;
import org.scify.democracit.wordcloud.utils.Configuration;

/**
 * execute serially for all consultations: remove extracted tokens, and generate
 * and insert new.
 */
@WebServlet(name = "ResetExtractor", urlPatterns = {"/ResetExtractor"})
public class ResetExtract extends HttpServlet {

    private static final long serialVersionUID = 1L;
    /**
     * the current directory
     */
    public String workingDir;
    // the properties file name
    public static final String PROPERTIES = "wordcloud.properties";
    private ILogger logger;

    private EntityManagerFactory emf;
    public static final String PERSISTENCE_RESOURCE = "org.scify_DemoModel_jar_0.1-SNAPSHOTPU";

    @Override
    public void init() throws ServletException {
        // init persistence manager
        emf = Persistence.createEntityManagerFactory(PERSISTENCE_RESOURCE);
//        // debug
        logger = new BaseEventLogger();
        // init logging
//        logger = DBAJPAEventLogger.getInstance(emf);
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
        String code = request.getParameter("reset_code");
        Configuration configuration = loadConfig(getServletContext());
        if (!code.equals(configuration.getResetCode())) {
            throw new IllegalArgumentException("invalid reset code");
        }
        // acquire module name
        String sModulName = configuration.getModuleName();

        IWordCloudDBA storage;
        IWordCloudExtractor extractor;
        ICommentsRetriever comments_retriever;
        ConsultationJpaController cons_con = new ConsultationJpaController(emf);
        CommentTermJpaController term_controller = new CommentTermJpaController(emf);
        Collection<Comments> cComments = new ArrayList();
        try {
            out = new PrintStream(response.getOutputStream());
            // load all consultations
            // init storage module
            storage = new JPAWordCloud(emf, logger);
            // initialize content/segment fetcher module
            comments_retriever = CommentsJPARetriever.getInstance(emf);

            List<Consultation> allConsultations = cons_con.findConsultationEntities();

            for (Consultation cons : allConsultations) {
                consultation_id = cons.getId().intValue();
                // delete all comment_terms for this consultation
                Collection<CommentTerm> findCommentTermsPerConsultation = term_controller.findCommentTermsPerConsultation(cons);
                for (CommentTerm each : findCommentTermsPerConsultation) {
                    try {
                        term_controller.destroy(each.getId());
                    } catch (NonexistentEntityException ex) {
                        logger.error(-1l, ex);
                    }
                }
                // load all comments for this consultation
                cComments = comments_retriever.getCommentsPerConsultationID(consultation_id);
                cComments = cleanComments(cComments);
                // load extractor. 
                extractor = new InRAMWordCloudExtractor(storage, configuration, logger);
                // generate the term - freq map and store to DB
                extractor.generateWordCloud(cComments, consultation_id, true);
            }
            out.print(new JSONMessage("OK").toJSON());
        } catch (IOException | SQLException ex) {
            // register activity error
            logger.error(-1l, ex);
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
        Configuration configuration = new Configuration(workingDir + ResetExtract.PROPERTIES);
        // set config working Directory
        configuration.setWorkingDir(workingDir);
        return configuration;
    }

    public static final String DIR_SEP = System.getProperty("file.separator");
}
