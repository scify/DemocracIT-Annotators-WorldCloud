/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.scify.democracit.dao.model.Comments;
import org.scify.democracit.demoutils.general.CollectionsCalcs;
import org.scify.democracit.wordcloud.dba.IWordCloudDBA;
import org.scify.democracit.wordcloud.freqextract.IFreqExtractor;

import org.scify.democracit.wordcloud.lemmatization.DummyGreekLemmatizer;
import org.scify.democracit.wordcloud.lemmatization.ILemmatizer;
import org.scify.democracit.demoutils.logging.ILogger;
import org.scify.democracit.wordcloud.model.TermResult;
import org.scify.democracit.wordcloud.model.TermToStore;
import org.scify.democracit.wordcloud.freqextract.SimpleFrequencyExtractor;
import org.scify.democracit.wordcloud.utils.Configuration;

/**
 *
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class InRAMWordCloudExtractor implements IWordCloudExtractor {

    /**
     * self explainable. Unlemmatized single word tokens
     */
    protected HashMap hmUnLemmatizedOneWordTerms;
    /**
     * Lemmatized single word tokens
     */
    protected HashMap hmLemmatizedOneWordTerms;
    /**
     * Lemmatized two word tokens
     */
    protected HashMap hmLemmatizedTwoWordTerms;
    /**
     * will hold the content ID's to update service_pl
     */
    protected Set<Long> sContentIDs;
    /**
     * the DB access module
     */
    protected IWordCloudDBA storage;
    /**
     * stopWords
     */
    protected Set<String> elStopWords;
    protected int maxStorageThreshold;
    protected static final String JSON_STORE_PATH = "/home/gkioumis/Documents/SciFY/DemocracIT/jsonTerms/";
    protected ILemmatizer grLemmatiser;
    protected Configuration conf;
    protected String lang = "el";
    /**
     * represents the total amount of topN bigrams to keep.
     */
    protected int iTopNBiGrams;

    protected ILogger logger;

    /**
     *
     * @param storage The DBA module
     * @param config the Configuration class
     * @param logger the Logger implementation
     * @throws java.io.IOException
     */
    public InRAMWordCloudExtractor(IWordCloudDBA storage,
            Configuration config, ILogger logger) throws IOException {

        this.logger = logger;
        this.storage = storage;
        // load stopWords
        this.elStopWords = new HashSet<>(FileUtils.readLines(config.getGRStopwordsFile(), Configuration.UTF8));
        // get max storage counter defined from file
        this.maxStorageThreshold = config.getmaxStoreCount();
        // init greek lemmatizer (check if libJLemma.so is loaded or load it)
        initGreekLemmatizer(config);
        // determine top-N bi-word tokens to keep
        this.iTopNBiGrams = config.getTopNBiGramsToStore();
    }

    /**
     * Runs in RAM.
     *
     * @param commentsList the comments to process
     * @param id the consultation ID (if the collection comes from a
     * consultation) or the article id (if the collection comes from an article)
     * @param isConsultation
     * @throws java.io.IOException
     */
    @Override
    public void generateWordCloud(Collection<Comments> commentsList, long id, boolean isConsultation) throws IOException, SQLException {
        // init maps
        hmUnLemmatizedOneWordTerms = new HashMap<>();
        hmLemmatizedTwoWordTerms = new HashMap<>();
        hmLemmatizedOneWordTerms = new HashMap<>();
        // init result map 
        LinkedHashMap<String, Double> hmRes = null;
        IFreqExtractor freqExtractor;
        // init term freq map
        Map<String, Float> freqMap;
        Set<String> stopWordsSet;
        // init tokenizer parameters
        int freqThreshold = 1; // set to 1
        boolean removeStopWords; // init to true, change afterwards
        int nGramOrder; // init to one Word tokens, change afterwards
        // ------------------------------------------------------------------ //
        // - first get unlemmatized one word tokens
        // - then get lemattized one word tokens
        // - last get unlemmatized bi-word tokens
        // ------------------------------------------------------------------ //
        if (commentsList != null && !commentsList.isEmpty()) {
            // verbosity
            int iTotal = commentsList.size(), i = 0;
            logger.info(String.format("Processing %d comments...%n", iTotal));
            for (Comments eachCand : commentsList) {
                i++; // update counter
                if (i % 1000 == 0) {
                    String sout = MessageFormat.format("Completed {0,number,#.##%}...",
                            (float) (i / (iTotal + 0.01)));
                    logger.info(sout);
                }
                // get Content attributes
                String sComment = eachCand.getComment();
                long lCommentID = eachCand.getId();
                // nGramOrder = 1 means that lemmatisers will be active when possible.
                nGramOrder = 1;
                removeStopWords = true;
                freqExtractor = initFreqExtractor(lang, nGramOrder, false);
                // select stopwords path
                stopWordsSet = selectStopWordsSet(lang);
                // if using stopwords
                if (stopWordsSet != null) {
                    // 1st: get the frequencies map for the segment, NOT LEMMATIZED
                    freqMap = freqExtractor.run(sComment,
                            stopWordsSet,
                            removeStopWords,
                            freqThreshold,
                            nGramOrder);
                    // save term freqs for that segment (in temp table)
                    processCommentOnTempTable(new TermResult(freqMap, lCommentID), nGramOrder);
                    // 2nd: get the frequencies map for the segment, lemmatized this time
                    freqExtractor = initFreqExtractor(lang, nGramOrder, true);
                    freqMap = freqExtractor.run(sComment,
                            stopWordsSet,
                            removeStopWords,
                            freqThreshold,
                            nGramOrder);
                    // save term freqs for that segment (in temp table)
                    processComment(new TermResult(freqMap, lCommentID), nGramOrder);
                }
                // 3rd: Also extract 2-grams as well  // TODO: check lemmatized
                nGramOrder = 2;
                removeStopWords = false; // do not remove stopwords. Ignore them afterwards
                freqExtractor = initFreqExtractor(lang, nGramOrder, true); // TODO: use false for unlemmatized
                freqMap = freqExtractor.run(sComment,
                        stopWordsSet,
                        removeStopWords,
                        freqThreshold,
                        nGramOrder); // Bigrams
                // save bi-term freqs for that segment
                processComment(new TermResult(freqMap, lCommentID), nGramOrder);
            }
            logger.info("Maps generated!");
            // calculate Bi-Grams
            hmRes = calculateBiGramTerms(id, 1, commentsList.size(), isConsultation);
            // store results.
            calculateAndStoreResults(hmRes);
        } else { // if no segments fetched, abort
            logger.info("No comments found to process.");
        }
    }

    /**
     * generates a list of {@link TermToStore} items (containing only bi-grams),
     * that match the bi-gram candidates
     *
     * @param sucCands the bi-gram candidates
     * @param termMap the full term map.
     * @return
     */
    private ArrayList<TermToStore> generateTermToStoreList(ArrayList<String> sucCands,
            HashMap<Long, ArrayList<TermResult>> termMap) {
        // generate the list that will be stored
        ArrayList<TermToStore> lsRes = new ArrayList<>();
        for (Map.Entry entry : termMap.entrySet()) {
            Long ID = (Long) entry.getKey();
            ArrayList<? extends TermResult> eachFreqMap
                    = (ArrayList<? extends TermResult>) entry.getValue();
            // for each result
            for (TermResult termResult : eachFreqMap) {
                // if term map contains a bi-token of the list provided
                for (String eachBiGram : sucCands) {
                    if (termResult.getTermFrequencyMap().containsKey(eachBiGram)) {
                        // get freq for that ID
                        Float freq = termResult.getTermFrequencyMap().get(eachBiGram);
                        // create a temp storable item
                        TermToStore tmpTerm = new TermToStore(ID, eachBiGram, freq);
                        // add it to the ToStore list
                        lsRes.add(tmpTerm);
                    }
                }
            }
        }
        return lsRes;
    }

    /**
     * generates a list of {@link TermToStore} items (containing one-grams) that
     * are not contained in the bi-gram candidates.
     *
     * @param hmLemmatizedOneWordTerms the full map
     * @param sucCands the list of bi-grams
     * @return
     */
    private ArrayList<TermToStore> generateTermToStoreList(HashMap<Long, ArrayList<TermResult>> hmLemmatizedOneWordTerms,
            ArrayList<String> sucCands) {

        // construct a set of strings contained in the bi-grams
        // -- these terms will be not stored as single terms, because they will be stored as 
        // bigrams alone.
        Set<String> sRemove = new HashSet<>();
        for (String biGram : sucCands) {
            String[] sOnes = biGram.split("\\s+");
            for (String each : sOnes) {
                sRemove.add(each.trim());
            }
        }
        // init result list: will contain all items to be later stored in storage layer
        ArrayList<TermToStore> lsRes = new ArrayList<>();
        // for each map entry (commentID, term result list)
        for (Map.Entry<Long, ArrayList<TermResult>> entry : hmLemmatizedOneWordTerms.entrySet()) {
            Long ID = entry.getKey();
            ArrayList<TermResult> freqMap = entry.getValue();
            // for each term - frequency map
            for (Iterator<TermResult> trIter = freqMap.iterator(); trIter.hasNext();) {
                Long curID = ID;
                TermResult termResult = trIter.next();
                for (Map.Entry termEntry : termResult.getTermFrequencyMap().entrySet()) {
                    String term = (String) termEntry.getKey();
                    Float freq = (Float) termEntry.getValue();
                    if (!sRemove.contains(term.trim())) {
                        // generate the storable object
                        TermToStore tmpTerm = new TermToStore(curID,
                                term, freq);
                        lsRes.add(tmpTerm);
                    }
                }
                trIter.remove();
            }
        }
        return lsRes;
    }

    /**
     *
     * @param sLang the language
     * @return the set or null if lang parameter is other than en, el, de
     */
    protected Set<String> selectStopWordsSet(String sLang) {
        Set<String> stopWordsSet = null;
        if (sLang.trim().equalsIgnoreCase("el")) {
            stopWordsSet = elStopWords;
        } else if (sLang.trim().equalsIgnoreCase("de")) {
            stopWordsSet = null;
        } else if (sLang.trim().equalsIgnoreCase("en")) {
            stopWordsSet = null;
        }
        return stopWordsSet;
    }

    /**
     * IN RAM
     *
     * @param trTerm
     * @param nGramOrder
     */
    protected void processCommentOnTempTable(TermResult trTerm, int nGramOrder) {
        // get the comment ID that the term belongs to
        long commentID = trTerm.getID();
        ArrayList<TermResult> lsTerms;
        if (!hmUnLemmatizedOneWordTerms.containsKey(commentID)) {
            lsTerms = new ArrayList<>();
            lsTerms.add(trTerm);
            hmUnLemmatizedOneWordTerms.put(commentID, lsTerms);
        } else {
            // update list with new entry
            lsTerms = (ArrayList<TermResult>) hmUnLemmatizedOneWordTerms.get(commentID);
            lsTerms.add(trTerm);
            hmUnLemmatizedOneWordTerms.put(commentID, lsTerms);
        }
    }

    /**
     * IN RAM
     *
     * @param trTerm
     * @param nGramOrder
     */
    protected void processComment(TermResult trTerm, int nGramOrder) {
        // get the comment ID that the term belongs to
        long commentID = trTerm.getID();
        // choose map
        ArrayList<TermResult> lsTerms;
        if (nGramOrder == 1) { // work on the unlemmatized
            if (!hmLemmatizedOneWordTerms.containsKey(commentID)) {
                lsTerms = new ArrayList<>();
                lsTerms.add(trTerm);
                hmLemmatizedOneWordTerms.put(commentID, lsTerms);
            } else {
                // update list with new entry
                lsTerms = (ArrayList<TermResult>) hmLemmatizedOneWordTerms.get(commentID);
                lsTerms.add(trTerm);
                hmLemmatizedOneWordTerms.put(commentID, lsTerms);
            }
        } else if (nGramOrder == 2) {
            if (!hmLemmatizedTwoWordTerms.containsKey(commentID)) {
                lsTerms = new ArrayList<>();
                lsTerms.add(trTerm);
                hmLemmatizedTwoWordTerms.put(commentID, lsTerms);
            } else {
                // update list with new entry
                lsTerms = (ArrayList<TermResult>) hmLemmatizedTwoWordTerms.get(commentID);
                lsTerms.add(trTerm);
                hmLemmatizedTwoWordTerms.put(commentID, lsTerms);
            }
        } else {
            throw new IllegalArgumentException("nGramOrder = 1 OR 2 pls");
        }
    }

    private LinkedHashMap<String, Double> calculateBiGramTerms(long id, int freqThreshold, int corpusSize, boolean IsConsultation) {

        // init result map
        HashMap<String, Double> pmiBiTerms = new HashMap<>();
        HashMap<String, Double> norm_pmiBiTerms = new HashMap<>();
        // obtain one word token frequencies
        logger.info("Generating one word token frequencies...");
        HashMap<String, Integer> hsOneWordTokens = getDocumentCountPerTermFromCorpus(hmLemmatizedOneWordTerms);
        logger.info("Generating one word token frequencies... Done.");
        // obtain two word token frequencies
        logger.info("Generating two word token frequencies...");
        HashMap<String, Integer> hsTwoWordTokens = getDocumentCountPerTermFromCorpus(hmLemmatizedTwoWordTerms);
        logger.info("Generating two word token frequencies... Done.");
        logger.info("Calculating NPMI for bi-grams...");
        // For each bi-gram
        for (Map.Entry<String, Integer> entry : hsTwoWordTokens.entrySet()) {
            // get vars
            String biTerm = entry.getKey(); // the biWord candidate
            Integer biFreq = entry.getValue(); // it's frequency in the corpus
            // Take into account only bigrams that occur in more than
            // (freqThreshold) documents
            if (biFreq > freqThreshold) {
                // Get 1st and 2nd word freq in corpus
                String wordA, wordB;
                String[] biTermTokens = biTerm.split("\\s+");
                Double pmi, n_pmi; // PMI, Normalized PMI
                if (biTermTokens.length > 1) {
                    wordA = biTermTokens[0];
                    wordB = biTermTokens[1];
                    Integer aFreq = hsOneWordTokens.get(wordA);
                    Integer bFreq = hsOneWordTokens.get(wordB);
                    if (aFreq != null && bFreq != null) {
                        pmi = calculatePMI(aFreq, bFreq, biFreq, corpusSize);
                        n_pmi = calculateNPMI(pmi, biFreq, corpusSize);
                        if (pmi > 10e-5) { // ignore 0 PMI
                            pmiBiTerms.put(biTerm, pmi);
                        }
                        if (n_pmi > 10e-5) {
                            norm_pmiBiTerms.put(biTerm, n_pmi);
                        }
                    } else {
                        String nullWordFreq = (aFreq == null) ? wordA : (bFreq == null) ? wordB : "";
                        logger.warn("\tNo occurence for " + nullWordFreq);
                    }
                } else {
                    logger.warn("\tWUT: length! " + biTermTokens.length);
                }
            }
        }
        logger.info("Calculating NPMI for bi-grams... Done.");
        // sort map by values
        LinkedHashMap<String, Double> lhmResNorm
                = (LinkedHashMap<String, Double>) CollectionsCalcs.sortMapByValues(norm_pmiBiTerms, false);
        // proceed with storage operations // TODO implement
//        storeResults(lhmResNorm);
        return lhmResNorm;
    }

    /**
     *
     * @param lhmResNorm the full map of bi-grams extracted
     */
    public void calculateAndStoreResults(LinkedHashMap<String, Double> lhmResNorm) throws SQLException {
        // debug
//        System.out.println("Storing to file...");
//        // store both files (tests)
//        String name = "comments";
//        Store.storeFullObjectToFile(lhmResNorm, TEST_FOLDER + name + "_" + new Date() + "_NPMI.json");
        // debug
        // define top N biGrams to keep
        int iCount = iTopNBiGrams;
        // keep bi-word tokens that meet up a certain condition:
        // keep only topN_biGrams first bi-word tokens with highest npmi from the map
        ArrayList<String> sucCands = new ArrayList<>();
        logger.info(String.format("Calculating Top %d Bi-Grams to store...%n", iCount));
        Iterator nmpiGramsIt = lhmResNorm.keySet().iterator();
        // construct a list with only these topN_biGrams
        while (nmpiGramsIt.hasNext() && iCount > 0) {
            String eachBiGram = (String) nmpiGramsIt.next();
            sucCands.add(eachBiGram);
            iCount--;
        }
        logger.info("Done");
        // store lemmatized single word tokens
        ArrayList<TermToStore> lsRes;
        // calculate one-word tokens to store : Omits one word tokens that are contained in the bi-grams.
        logger.info("Calculating single terms to store in DB...");
        lsRes = generateTermToStoreList(hmLemmatizedOneWordTerms, sucCands);
        hmLemmatizedOneWordTerms.clear(); // clear RAM
        hmUnLemmatizedOneWordTerms.clear();
        logger.info("Done");
        // store one word terms
        storage.batchStoreTerms(lsRes, 1);
//        for (TermToStore termToStore : lsRes) {
//            storage.storeTerm(termToStore.getTerm(), termToStore.getFreq(), termToStore.getCommentID(), 1);
//        }
        // calculate two-word tokens to store
        logger.info("Calculating two terms to store in DB...");
        lsRes = generateTermToStoreList(sucCands, hmLemmatizedTwoWordTerms);
        hmLemmatizedTwoWordTerms.clear(); // clear RAM
        logger.info("Done");
        // debug
//        System.out.println(lsRes);

        // store the bi-gram list to the DB
        storage.batchStoreTerms(lsRes, 2);
    }

    private <T> HashMap<String, Integer> getDocumentCountPerTermFromCorpus(HashMap<Long, ArrayList<T>> corpus) {
        // init result map
        HashMap<String, Integer> hmRes = new HashMap<>();
        for (ArrayList<T> tmpTerms : corpus.values()) {
            // for each term - freq map
            for (T conRes : tmpTerms) {
                if (conRes instanceof TermResult) {
                    TermResult tmpTermRes = (TermResult) conRes;
                    // get term - freq map
                    Map<String, Float> tmpfreqmap = tmpTermRes.getTermFrequencyMap();
                    // for each term, update collecting term-freq Map
                    for (Map.Entry<String, Float> entry : tmpfreqmap.entrySet()) {
                        String tmpTerm = entry.getKey();
                        // Just count 1 per document
                        Integer tmpFreq = entry.getValue() > 0.0 ? 1 : 0;
                        // update collecting map
                        if (!hmRes.containsKey(tmpTerm)) {
                            hmRes.put(tmpTerm, tmpFreq);
                        } else {
                            hmRes.put(tmpTerm, hmRes.get(tmpTerm) + tmpFreq);
                        }
                    }
                }
            }
        }
        return hmRes;
    }

    private <T> HashMap<String, Integer> getTermFreqsFromCorpus(HashMap<Long, ArrayList<T>> corpus) {
        // init result map
        HashMap<String, Integer> hmRes = new HashMap<>();
        for (ArrayList<T> tmpTerms : corpus.values()) {
            // for each term - freq map
            for (T conRes : tmpTerms) {
                if (conRes instanceof TermResult) {
                    TermResult tmpTermRes = (TermResult) conRes;
                    // get term - freq map
                    Map<String, Float> tmpfreqmap = tmpTermRes.getTermFrequencyMap();
                    // for each term, update collecting term-freq Map
                    for (Map.Entry<String, Float> entry : tmpfreqmap.entrySet()) {
                        String tmpTerm = entry.getKey();
                        Float tmpFreq = entry.getValue();
                        // update collecting map
                        if (!hmRes.containsKey(tmpTerm)) {
                            hmRes.put(tmpTerm, tmpFreq.intValue());
                        } else {
                            hmRes.put(tmpTerm, hmRes.get(tmpTerm) + tmpFreq.intValue());
                        }
                    }
                }
            }
        }
        return hmRes;
    }

    /**
     * TODO: change the freq calcs.
     *
     * @param aFreq the frequency (number of documents found in / total number
     * of documents) of the token a
     * @param bFreq the frequency of the token b
     * @param biFreq the frequency of the both the tokens together (as a 2-word
     * token)
     * @param dCorpus the corpus size
     * @return the PMI of the biWord
     * @see <a
     * href="http://en.wikipedia.org/wiki/Pointwise_mutual_information">Pointwise
     * mutual information</a>
     */
    protected double calculatePMI(Integer aFreq, Integer bFreq, Integer biFreq, double dCorpus) {
        double denominator = (((double) aFreq / dCorpus) * ((double) bFreq / dCorpus));
        return Math.log(((double) biFreq / dCorpus) / denominator);
    }

//    protected Double calculateNPMI(Integer aFreq, Integer bFreq, Integer biFreq, int dCorpus) {
//        // see 3.1 in https://svn.spraakdata.gu.se/repos/gerlof/pub/www/Docs/npmi-pfd.pdf
//        double log_denominator = (((double)aFreq/dCorpus) * ((double)bFreq/dCorpus));
//        return Math.log(((double) biFreq/dCorpus) / log_denominator) / (- Math.log(((double) biFreq/dCorpus)));
//    }
    /**
     *
     * @param pmi the calculated PMI
     * @param biFreq the frequency found of the bi-gram
     * @param dCorpus the corpus size
     * @return the Normalized PMI value of the bi-gram
     */
    protected Double calculateNPMI(double pmi, Integer biFreq, int dCorpus) {
//        return pmi / (- Math.log(((double) biFreq/dCorpus)));
        return biFreq * pmi;
    }

    private ILemmatizer chooseLemmatizer(Locale locale, boolean bLemmatizer) {
        if (!bLemmatizer) {
            return null;
        }
        ILemmatizer lem = null;
        if (locale.toString().equalsIgnoreCase("en")) {
            lem = null;
        } else if (locale.toString().equalsIgnoreCase("el")) {
            lem = grLemmatiser;
        } else if (locale.toString().equalsIgnoreCase("de")) {
            lem = null;
        }
        return lem;
    }

    /**
     *
     * @param sCandLang the language iso_code
     * @param nGramOrder
     * @param bLemmatizer
     * @return a {@link SimpleFrequencyExtractor} based on the language passed
     */
    protected IFreqExtractor initFreqExtractor(String sCandLang,
            int nGramOrder, boolean bLemmatizer) {
        // get locale from language
        Locale locale = (sCandLang.equalsIgnoreCase("el") ? new Locale("el", "GR")
                : sCandLang.equalsIgnoreCase("en") ? Locale.ENGLISH : Locale.GERMAN);
        // init tokenizer with locale and language specific lemmatizer
        return new SimpleFrequencyExtractor(locale, chooseLemmatizer(locale, bLemmatizer), logger);
    }

    private void initGreekLemmatizer(Configuration config) {
        if (true) {
//        if (config.shouldLoadLibJLemma()) {
            System.out.println("loading dummy greek lemmatizer");
            this.grLemmatiser = new DummyGreekLemmatizer();
        } else {
            System.out.println("loading dummy greek lemmatizer");
            this.grLemmatiser = new DummyGreekLemmatizer();
        }
    }
}
