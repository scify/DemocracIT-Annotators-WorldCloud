/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.model;

/**
 * depicts a storeable term result
 *
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class TermToStore {

    /**
     * comment commentID
     */
    private final Long commentID;
    /**
     * the actual term (1 / 2 gram)
     */
    private String term;
    /**
     * the term freq in the supplied commentID
     */
    private Float freq;

    public TermToStore(Long commentID, String term, Float freq) {
        this.commentID = commentID;
        this.term = term;
        this.freq = freq;
    }

    public Long getCommentID() {
        return commentID;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public Float getFreq() {
        return freq;
    }

    public void setFreq(Float freq) {
        this.freq = freq;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (this.commentID != null ? this.commentID.hashCode() : 0);
        hash = 37 * hash + (this.term != null ? this.term.hashCode() : 0);
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
        final TermToStore other = (TermToStore) obj;
        if (this.commentID != other.commentID && (this.commentID == null || !this.commentID.equals(other.commentID))) {
            return false;
        }
        if ((this.term == null) ? (other.term != null) : !this.term.equals(other.term)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TermToStore{" + "ID=" + commentID + ", term=" + term + ", freq=" + freq + '}';
    }
}
