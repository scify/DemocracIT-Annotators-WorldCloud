/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.lemmatization;

/**
 *
 * @author George K.<gkiom@iit.demokritos.gr>
 */
public class DummyGreekLemmatizer implements ILemmatizer {

    /**
     * Dummy impl.
     *
     * @param sToken the token to lemmatize
     * @return the token
     */
    @Override
    public String lemmatize(String sToken) {
        return sToken;
    }

}
