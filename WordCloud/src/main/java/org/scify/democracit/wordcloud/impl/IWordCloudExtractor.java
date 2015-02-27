/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import org.scify.democracit.model.Comment;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public interface IWordCloudExtractor {

    /**
     * generates the term - frequency map, ordered by frequencies
     *
     * @param cComments the collection of comments to process
     * @param id the consultation / article id
     * @param isConsultation true if the comments belong to a consultation,
     * false if they belong only in an article.
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     */
    public void generateWordCloud(Collection<Comment> cComments, long id, boolean isConsultation) throws IOException, SQLException;

}
