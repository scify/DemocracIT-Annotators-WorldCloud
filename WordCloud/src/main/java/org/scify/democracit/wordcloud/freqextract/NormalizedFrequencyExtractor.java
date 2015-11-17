package org.scify.democracit.wordcloud.freqextract;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.scify.democracit.wordcloud.lemmatization.ILemmatizer;
import org.scify.democracit.demoutils.logging.ILogger;
import org.scify.democracit.wordcloud.tokenization.ITokenizer;
import org.scify.democracit.wordcloud.tokenization.NormalizedNGramTokenizer;
import org.scify.democracit.wordcloud.utils.Configuration;
import org.scify.democracit.wordcloud.utils.StringUtils;

/**
 * Performs simple term frequency extraction
 *
 * @author George K. <gkiom@scify.org>
 */
public class NormalizedFrequencyExtractor implements IFreqExtractor {

    private Set<String> stopwords;
    private final Locale loc;
    private final ILemmatizer lemmatizer;
    private ITokenizer tokenizer;
    private ILogger logger;

    /**
     * Constructor with no lemmatization
     *
     * @param locale the locale
     * @param logger
     */
    public NormalizedFrequencyExtractor(Locale locale, ILogger logger) {
        this.loc = locale;
        this.lemmatizer = null;
        this.logger = logger;
        this.stopwords = new HashSet();
    }

    /**
     * Constructor with lemmatization applied before returing the term -
     * frequency map. Can also accept null lemmatizer (will not lemmatize)
     *
     * @param locale the locale
     * @param lemmatizer the lemmatizer to load
     * @param logger
     */
    public NormalizedFrequencyExtractor(Locale locale, ILemmatizer lemmatizer, ILogger logger) {
        this.loc = locale;
        this.lemmatizer = lemmatizer;
        this.logger = logger;
        this.stopwords = new HashSet();
    }

    /**
     *
     * @param content The text that we need to clean and tokenize.
     * @param stopWordsSet A set containing the stopwords.
     * @param removeStopWords True if stopwords should be removed.
     * @param freqThreshold The threshold above which a words should be kept as
     * a token.
     * @param nGramOrder The n-gram order to check for tokenization.
     * @return A map of tokens to their frequencies.
     */
    @Override
    public Map<String, Float> run(String content,
            Set<String> stopWordsSet,
            boolean removeStopWords,
            int freqThreshold,
            int nGramOrder)
            throws IOException {

        Map<String, Double> sentenceHashMap = new HashMap();
        Map<String, Float> finalHashMap = new HashMap();

        initStopwords(removeStopWords, stopWordsSet, nGramOrder);

        // init tokenizer
        this.tokenizer
                = new NormalizedNGramTokenizer(removeStopWords, stopwords, loc);

        // Read all the lines from the content
        List<String> lines = null;
        lines
                = IOUtils.readLines(new InputStreamReader(IOUtils.toInputStream(content, Configuration.UTF8)));
        if (lines == null) {
            return null;
        }
        // For every line
        for (String line : lines) {
            // tokenize it
            List<String> tokens = tokenizer.tokenize(line, nGramOrder);
            String token;
            String ngram;
            // for every token
            for (int t = 0; t < tokens.size(); t++) {
                token = tokens.get(t);
                // Ignore single letter tokens
                // TAKEN CARE of in NGramTokenizer for nGramOrder = 1
                // if a bi-word token
                if (nGramOrder == 2) {
                    String[] words = token.split("\\s+");
                    String wordA = words[0];
                    String wordB = words[1];
                    // ignore whole token if one word is a single letter.
                    if (wordA.length() < 2 || wordB.length() < 2) {
                        continue;
                    }
                    // ignore token if words are equal
                    if (wordA.equalsIgnoreCase(wordB)) {
                        continue;
                    }
                    if (!removeStopWords && stopwords != null) {
                        // ignore bi-grams containing STOPWORDS
                        if (stopwords.contains(StringUtils.normalize(wordA))
                                || stopwords.contains(StringUtils.normalize(wordB))) {
                            continue;
                        }
                    }
                }
                // Check whether the token exists in the
                // token-frequency map
                ngram = token;
                Double sentenceTokenCount = sentenceHashMap.get(ngram);
                // If it does not, initialize its value
                if (sentenceTokenCount == null) {
                    sentenceTokenCount = 0.0;
                }
                double temp = sentenceTokenCount + 1.0;
                // Update the map
                sentenceHashMap.put(ngram, temp);
            }
        }

        // declare lemma
        String sLemma;
        // For each entry
        for (Map.Entry<String, Double> eCur : sentenceHashMap.entrySet()) {
            // If it exceeds the threshold
            if (eCur.getValue() >= freqThreshold) // add it to the returned map
            {
                // get token
                String sToken = eCur.getKey();
                // lemmatize, if possible
                if (this.lemmatizer != null) {
                    sLemma = lemmatizer.lemmatize(sToken);
                } else {
                    sLemma = sToken;
                }
                // update final map
                if (finalHashMap.containsKey(sLemma)) {
                    // add to it's current freq
                    finalHashMap.put(sLemma, finalHashMap.get(sLemma) + eCur.getValue().floatValue());
                } else {
                    // put to lemmatized map with it's frequency
                    finalHashMap.put(sLemma, eCur.getValue().floatValue());
                }
            }
        }
        return finalHashMap;
    }

    private void initStopwords(boolean removeStopWords, Set<String> stopWordsSet, int nGramOrder) throws IllegalArgumentException {
        Set<String> normalized_stopwords = new HashSet();
        // If we should remove stopwords
        if (removeStopWords) {
            if (stopWordsSet == null || stopWordsSet.isEmpty()) {
                throw new IllegalArgumentException("if called with 'removeStopWords=true, you MUST provide a stopword set");
            }
            for (String each : stopWordsSet) {
                normalized_stopwords.add(StringUtils.normalize(each));
            }
            // update the list of stopwords
            stopwords = normalized_stopwords;
        }
        if (!removeStopWords && (nGramOrder == 2 && !stopWordsSet.isEmpty())) {
            // update stopword set for bigram extraction as well
            for (String each : stopWordsSet) {
                normalized_stopwords.add(StringUtils.normalize(each));
            }
            // update the list of stopwords
            stopwords = normalized_stopwords;
        }
    }
}
