/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.dba;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import javax.sql.DataSource;
import org.scify.democracit.demoutils.DataAccess.DBUtils.SQLUtils;
import org.scify.democracit.demoutils.logging.ILogger;
import org.scify.democracit.wordcloud.model.TermToStore;
import org.scify.democracit.wordcloud.response.WordCloudResponse;
import org.scify.democracit.wordcloud.utils.Configuration;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public class PSQLWordCloudDBA extends WordCloudDBA implements IWordCloudDBA {

    private final int iBatchLimit;

    /**
     * Default Constructor.
     *
     * @param dataSource
     * @param config
     * @param logger
     */
    public PSQLWordCloudDBA(DataSource dataSource, Configuration config, ILogger logger) {
        super(dataSource, logger);
        this.iBatchLimit = config.getmaxStoreCount();
    }

    @Override
    public void storeTerm(String term, Float frequency, long commentID, int nGramOrder) throws SQLException {
        String sql = "INSERT INTO comment_term (term_string, term_frequency, n_gram_order, comment_id) VALUES(?,?,?,?);";
        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        try {
            dbConnection = dataSource.getConnection();
            preparedStatement = dbConnection.prepareStatement(sql);
            preparedStatement.setString(1, term);
            preparedStatement.setInt(2, frequency.intValue());
            preparedStatement.setInt(3, nGramOrder);
            preparedStatement.setLong(4, commentID);
            preparedStatement.executeUpdate();
        } finally {
            SQLUtils.release(dbConnection, preparedStatement, null);
        }
    }

    @Override
    public boolean containsTerm(String term, Float frequency, long commentID) throws SQLException {
        boolean contains = false;
        String sql = "SELECT id FROM comment_term WHERE comment_id = ? AND term_string = ?;";
        Connection dbConnection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbConnection = dataSource.getConnection();
            preparedStatement = dbConnection.prepareStatement(sql);
            preparedStatement.setLong(1, commentID);
            preparedStatement.setString(2, term);
            preparedStatement.execute();
            resultSet = preparedStatement.getResultSet();
            if (resultSet.next()) {
                contains = (resultSet.getInt("id") > 0);
            }
        } finally {
            SQLUtils.release(dbConnection, preparedStatement, resultSet);
        }
        return contains;
    }

    /**
     * Uses simple list iteration (not batch updates)
     *
     * @param lsToStore the list of terms to store
     * @param nGramOrder 1 | 2
     */
    @Override
    public void batchStoreTerms(List<TermToStore> lsToStore, int nGramOrder) throws SQLException {
        logger.info(String.format("Storing %d comments_terms in DB...", lsToStore.size()));
        for (TermToStore termToStore : lsToStore) {
            if (!containsTerm(termToStore.getTerm(), termToStore.getFreq(), termToStore.getCommentID())) {
                storeTerm(termToStore.getTerm(), termToStore.getFreq(), termToStore.getCommentID(), nGramOrder);
            }
        }
        logger.info(String.format("Storing %d comments_terms in DB...Done", lsToStore.size()));
    }

    @Override
    public WordCloudResponse loadTermCloud(int process_id, boolean isConsultation, int limit, int n_gram_order) throws SQLException {
        logger.info(String.format("loading term cloud for %s ID : %d - max: %d terms", 
                isConsultation ? "consultation" : "article", process_id, limit));
        LinkedHashMap<String, Double> res = new LinkedHashMap();
        
        String sql = "SELECT term_string, sum(term_frequency) as total_freq "
                + "FROM comment_term "
                + "INNER JOIN comments ON comments.id = comment_term.comment_id "
                + "INNER JOIN articles ON articles.id = comments.article_id ";
        sql += isConsultation ? "INNER JOIN consultation ON consultation.id = articles.consultation_id " : "";
        sql += isConsultation ? "WHERE consultation.id = ? " : "WHERE articles.id = ? ";
        sql += n_gram_order == 0 ? "" : "AND n_gram_order = ? ";
        sql += "GROUP BY term_string ORDER BY total_freq DESC LIMIT ?;";

        Connection dbConnection = null;
        PreparedStatement pStmt = null;
        ResultSet rSet = null;
        try {
            dbConnection = dataSource.getConnection();
            pStmt = dbConnection.prepareStatement(sql);
            pStmt.setLong(1, process_id);
            if (n_gram_order != 0) {
                pStmt.setInt(2, n_gram_order);
                pStmt.setInt(3, limit);
            } else {
                pStmt.setInt(2, limit);
            }
            rSet = pStmt.executeQuery();
            while (rSet.next()) {
                res.put(rSet.getString(1), rSet.getDouble(2));
            }
        } finally {
            SQLUtils.release(dbConnection, pStmt, rSet);
        }
        WordCloudResponse response = new WordCloudResponse(res);
        return response;
    }
}
