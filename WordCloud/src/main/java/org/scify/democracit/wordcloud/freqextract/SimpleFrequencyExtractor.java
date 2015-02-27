package org.scify.democracit.wordcloud.freqextract;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.scify.democracit.wordcloud.lemmatization.ILemmatizer;
import org.scify.democracit.demoutils.logging.DBAEventLogger;
import org.scify.democracit.demoutils.logging.ILogger;
import org.scify.democracit.wordcloud.tokenization.ITokenizer;
import org.scify.democracit.wordcloud.tokenization.NGramTokenizer;
import org.scify.democracit.wordcloud.utils.Configuration;

/**
 * Performs simple term frequency extraction
 *
 * @author George K. <gkiom@scify.org>
 */
public class SimpleFrequencyExtractor implements IFreqExtractor {

    private Set<String> STOPWORDS;
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
    public SimpleFrequencyExtractor(Locale locale, ILogger logger) {
        this.loc = locale;
        this.lemmatizer = null;
        this.logger = logger;
        this.STOPWORDS = new HashSet<>();
    }

    /**
     * Constructor with lemmatization applied before returing the term -
     * frequency map. Can also accept null lemmatizer (will not lemmatize)
     *
     * @param locale the locale
     * @param lemmatizer the lemmatizer to load
     * @param logger
     */
    public SimpleFrequencyExtractor(Locale locale, ILemmatizer lemmatizer, ILogger logger) {
        this.loc = locale;
        this.lemmatizer = lemmatizer;
        this.logger = logger;
        this.STOPWORDS = new HashSet<>();
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

        Map<String, Double> sentenceHashMap = new HashMap<>();
        Map<String, Float> finalHashMap = new HashMap<>();

        // If we should remove stopwords
        if (removeStopWords) {
            // update the list of stopwords
            STOPWORDS = stopWordsSet;
        }

        // init tokenizer
        this.tokenizer
                = new NGramTokenizer(removeStopWords, STOPWORDS, loc);

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
            ArrayList<String> tokens = tokenizer.tokenize(line, nGramOrder);
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
                    if (!removeStopWords && stopWordsSet != null) {
                        // ignore bi-grams containing STOPWORDS
                        if (stopWordsSet.contains(wordA) || stopWordsSet.contains(wordB)) {
                            continue;
                        }
                    }
                }
                // Check whether the token exists in the
                // token-frequency map
                ngram = token;
                Double sentenceTokenCount = (Double) sentenceHashMap.get(ngram);
                // If it does not, initialize its value
                if (sentenceTokenCount == null) {
                    sentenceTokenCount = new Double(0.0);
                }
                double temp = sentenceTokenCount.doubleValue() + 1.0;
                // Update the map
                sentenceHashMap.put(ngram, new Double(temp));
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

    public static void main(String[] args) throws IOException {
        // test
        String s = "Two bodies were found but a further 10 people were missing after the incident near the island of Farmakonisi on Monday. "
                + "Sixteen people were rescued.\n"
                + "The coastguard says it judged it safer to keep migrants on their own boat than to take them aboard in bad weather.\n"
                + "Greek officials say panicking migrants caused the boat to capsize themselves.\n"
                + "According to reports, two migrants fell or dived overboard and others rushed to one side of the boat to rescue them, causing the boat to tip.\n"
                + "The deaths of a woman and a child have been confirmed.\n"
                + "One non-government organisation, Pro Asyl, accused the Greek authorities of trying illegally to prevent the migrants, believed to be 26 Afghans and two Syrians, landing in Greece.\n"
                + "\"It is highly likely that this action by the Greek coastguard was an illegal push-back operation rather than a rescue at sea,\" said Karl Kopp, the NGO's director of European affairs.\n"
                + "Another NGO, Ecre, said: \"Survivors tell that they were crying out for help, given that a large number of children and babies were on board.\"\n"
                + "Greece is one of the main destinations for clandestine migrants and refugees seeking to enter the EU, through its land or sea borders.\n"
                + "Correspondents say there has been a sharp increase in sea-borne refugee traffic over the past year because of stricter controls on the Greek-Turkish land border to the north and the ongoing war in Syria.";

        ILogger logger = new DBAEventLogger(null);
        IFreqExtractor extr = new SimpleFrequencyExtractor(Locale.ENGLISH, logger);
        Map test = extr.run(s, null, false, 1, 2);

        System.out.println(test.containsKey("a sharp"));

    }
}
