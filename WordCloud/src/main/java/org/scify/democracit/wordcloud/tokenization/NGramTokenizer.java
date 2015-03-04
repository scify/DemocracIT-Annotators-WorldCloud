package org.scify.democracit.wordcloud.tokenization;

import java.util.regex.Pattern;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.RegExFilteredTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class NGramTokenizer implements ITokenizer {

    private final boolean removeStopWords;
    private Set<String> stopWordSet;
    private Locale loc;

    public NGramTokenizer(boolean removeStopWords,
            Set<String> stopWordSet,
            Locale locale) {
        this.removeStopWords = removeStopWords;
        this.stopWordSet = stopWordSet;
        this.loc = locale;
    }

    @Override
    public ArrayList<String> tokenize(String input, int nGramOrder) {
        ArrayList<String> nGrams = new ArrayList<>();
        TokenizerFactory tf = IndoEuropeanTokenizerFactory.INSTANCE;
        Pattern pattern = Pattern.compile("[\\p{L}_]+|[\\!?}]+");
        RegExFilteredTokenizerFactory rtf = new RegExFilteredTokenizerFactory(tf, pattern);
        Tokenizer tokenizer;
        if (nGramOrder > 1) {
            String[] prevs = new String[nGramOrder - 1];
            for (int i = 0; i < prevs.length; i++) {
                prevs[i] = "";
            }
            tokenizer = rtf.tokenizer(input.toCharArray(), 0, input.length());

            String token = tokenizer.nextToken();
            String ngram;
            while (token != null) {
                token = token.trim().toLowerCase(loc);
                ngram = token;
                if (removeStopWords && stopWordSet.contains(token)) {
                    // ignore
                } else {

                    if (!prevs[prevs.length - 1].equals("")) {  ///find the ngram
                        //System.out.println("prevs full");
                        for (int i = prevs.length - 1; i >= 0; i--) {
                            if (!prevs[i].equals("")) {
                                ngram = prevs[i] + " " + ngram;
                            }
                        }
                        nGrams.add(ngram);

                        for (int j = 0; j < prevs.length - 1; j++) {
                            prevs[j] = prevs[j + 1];
                        }
                        prevs[prevs.length - 1] = token;
                    } else { //find the index to put the token
                        // System.out.println("prevs not full");
                        int tindex = prevs.length - 1;
                        for (int i = prevs.length - 2; i >= 0; i--) {
                            if (prevs[i].equals("")) {
                                tindex = i;
                            }
                        }
                        prevs[tindex] = token;

                    }
                }
                token = tokenizer.nextToken();
            }

        } else { // nGram = 1
            tokenizer = rtf.tokenizer(input.toCharArray(), 0, input.length());

            String token = tokenizer.nextToken();
//                        String toCheck;
            while (token != null) {
//                            token = token.trim();
//                            // trim and convert to lower
//                            toCheck = token.toLowerCase(loc);
                token = token.trim().toLowerCase(loc);
                // Ignore single letter tokens
                if (token.length() > 1) {

                    if (removeStopWords) {
                        if (!stopWordSet.contains(token)) {
                            nGrams.add(token);
                        }
                    } else {
                        nGrams.add(token);
                    }
                }
                token = tokenizer.nextToken();
            }
        }
        return nGrams;
    }
}
