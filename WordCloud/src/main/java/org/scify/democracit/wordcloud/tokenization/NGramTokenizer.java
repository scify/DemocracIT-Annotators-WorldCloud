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

    public static void main(String[] args) {

        ITokenizer tok = new NGramTokenizer(false, null, Locale.ENGLISH);
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

        System.out.println(tok.tokenize(s, 2));

    }
}
