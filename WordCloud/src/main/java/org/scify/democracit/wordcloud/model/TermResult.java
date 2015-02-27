/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.model;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class TermResult {

    /**
     * Represents the term - frequency
     */
    protected final Map<String, Float> termFreq;
    /**
     * The ID of this map, i.e. the comment that it is derived from
     */
    protected final long ID;

    /**
     *
     * @param termFreq the map of the token frequencies
     * @param commentID for a specific commment ID
     */
    public TermResult(Map<String, Float> termFreq, long commentID) {
        if (termFreq == null) {
            this.termFreq = new HashMap<>();
        } else {
            this.termFreq = termFreq;
        }
        this.ID = commentID;
    }

    /**
     * Indicates that the ID has been parsed but no terms have been extracted
     *
     * @param commentID the id passed
     */
    public TermResult(long commentID) {
        this.ID = commentID;
        this.termFreq = new HashMap<>(0);
    }

    public Map<String, Float> getTermFrequencyMap() {
        return termFreq;
    }

    /**
     *
     * @return the comment ID that the term-frequency map comes from
     */
    public long getID() {
        return ID;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (int) (this.ID ^ (this.ID >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TermResult other = (TermResult) obj;
        if (this.ID != other.ID) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TermResult{" + "termFreq=" + termFreq.toString() + ", commenrID=" + ID + '}';
    }

}
