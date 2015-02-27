/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.freqextract;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public interface IFreqExtractor {

    /**
     * Executes the frequency Extractor
     *
     * @param content the text to process
     * @param stopWordsSet stopwords, not to be taken into account.
     * @param removeStopWords true if we want stopwords to be removed (MUST be
     * supplied)
     * @param freqThreshold the cut-off threshold to keep a token (usually 1)
     * @param nGramOrder the n-gram order of tokenization (1 for single word
     * tokens, 2 for bi-grams, etc)
     * @return a map containing the term-frequency
     * @throws java.io.IOException
     */
    public Map<String, Float> run(String content,
            Set<String> stopWordsSet,
            boolean removeStopWords,
            int freqThreshold,
            int nGramOrder) throws IOException;
}
