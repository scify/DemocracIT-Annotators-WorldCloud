/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.response;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public class WordCloudResponse {

    private final List<TermFreqPair> results;

    public WordCloudResponse(LinkedHashMap<String, Double> hmRes) {
        this.results = new ArrayList<>();
        for (Map.Entry<String, Double> entry : hmRes.entrySet()) {
            String term = entry.getKey();
            Double freq = entry.getValue();
            this.results.add(new TermFreqPair(term, freq));
        }
    }

    public String toJSON() {
        return new Gson().toJson(this, WordCloudResponse.class);
    }

    class TermFreqPair {

        private String term;
        private Double freq;

        public TermFreqPair(String term, Double freq) {
            this.term = term;
            this.freq = freq;
        }

        public String getTerm() {
            return term;
        }

        public Double getFreq() {
            return freq;
        }

        @Override
        public String toString() {
            return "{" + "term=" + term + ", freq=" + freq + '}';
        }
    }

}
