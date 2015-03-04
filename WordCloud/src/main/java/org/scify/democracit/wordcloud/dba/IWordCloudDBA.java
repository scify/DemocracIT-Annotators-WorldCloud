/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.dba;

import java.sql.SQLException;
import java.util.List;
import org.scify.democracit.wordcloud.model.TermToStore;
import org.scify.democracit.wordcloud.response.WordCloudResponse;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public interface IWordCloudDBA {

    /**
     * Stores a single term in the DB
     *
     * @param term the term to store
     * @param frequency the term frequency
     * @param commentID where the term originates from
     * @param nGramOrder 1 for a single token, 2 for a bi-gram
     * @throws java.sql.SQLException
     */
    public void storeTerm(String term, Float frequency, long commentID, int nGramOrder) throws SQLException;

    /**
     *
     * @param term the term to store
     * @param commentID where the term originates from
     * @return true if the term (for the specified commentID) already exists in
     * the DB
     * @throws java.sql.SQLException
     */
    public boolean containsTerm(String term, long commentID) throws SQLException;

    public void updateTerm(String term, Float frequency, long commentID) throws SQLException;
    
    /**
     * Custom implementation (could use BatchUpdates)
     *
     * @param lsToStore the list of {@link TermToStore} items
     * @param nGramOrder the n-gram order of the terms. Must be 1 | 2
     * @throws java.sql.SQLException
     */
    public void batchStoreTerms(List<TermToStore> lsToStore, int nGramOrder) throws SQLException;
    
    public WordCloudResponse loadTermCloud(int process_id, boolean isConsultation, int max_terms, int n_gram_order) throws SQLException;
}
