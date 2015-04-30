/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.dba;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import org.scify.democracit.dao.jpacontroller.CommentTermJpaController;
import org.scify.democracit.dao.jpacontroller.CommentsJpaController;
import org.scify.democracit.dao.model.CommentTerm;
import org.scify.democracit.dao.model.Comments;
import org.scify.democracit.demoutils.logging.ILogger;
import org.scify.democracit.wordcloud.model.TermToStore;
import org.scify.democracit.wordcloud.response.WordCloudResponse;

/**
 *
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class JPAWordCloud implements IWordCloudDBA {

    private CommentTermJpaController controller;
    private CommentsJpaController commentsController;
    private ILogger logger;

    public JPAWordCloud(EntityManagerFactory emf, ILogger logger) {
        this.controller = new CommentTermJpaController(emf);
        this.commentsController = new CommentsJpaController(emf);
        this.logger = logger;
    }


    @Override
    public void storeTerm(String term, Float frequency, long commentID, int nGramOrder) throws SQLException {
        CommentTerm tmp = new CommentTerm();
        tmp.setTermString(term);
        tmp.setTermFrequency(frequency.intValue());
        Comments comment = commentsController.findComments(commentID);
        tmp.setCommentId(comment);
        tmp.setNGramOrder(new Integer(nGramOrder).shortValue());
        controller.create(tmp);
    }

    @Override
    public boolean containsTerm(String term, long commentID) throws SQLException {
        CommentTerm existing = controller.findByTermStringAndCommentID(term, commentID);
        return existing != null;
    }

    @Override
    public void updateTerm(String term, Float frequency, long commentID) throws SQLException {
        CommentTerm tmp = new CommentTerm();
        tmp.setTermString(term);
        tmp.setTermFrequency(frequency.intValue());
        CommentTerm existing = controller.findByTermStringAndCommentID(term, commentID);
        boolean update = existing.getTermFrequency().compareTo(frequency.intValue()) != 0;
        if (update) {
            Long id = existing.getId();
            tmp.setId(id);
            Comments comment = commentsController.findComments(commentID);
            tmp.setCommentId(comment);
            try {
                controller.edit(tmp);
            } catch (Exception ex) {
                Logger.getLogger(JPAWordCloud.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    @Override
    public void batchStoreTerms(List<TermToStore> lsToStore, int nGramOrder) throws SQLException {
        logger.info(String.format("Processing %d terms of n-gram %d...%n", lsToStore.size(), nGramOrder));
        // TODO: NOT REALLY THAT BATCH!
        for (TermToStore termToStore : lsToStore) {
            if (!containsTerm(termToStore.getTerm(), termToStore.getCommentID())) {
                storeTerm(termToStore.getTerm(), termToStore.getFreq(), termToStore.getCommentID(), nGramOrder);
            } else {
                updateTerm(termToStore.getTerm(), termToStore.getFreq(), termToStore.getCommentID());
            }
        }
    }

    @Override
    public WordCloudResponse loadTermCloud(int process_id, boolean isConsultation, int max_terms, int n_gram_order) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
