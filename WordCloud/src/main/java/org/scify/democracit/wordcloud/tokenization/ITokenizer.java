/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.tokenization;

import java.util.List;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public interface ITokenizer {

    public List<String> tokenize(String input, int nGramOrder);

}
