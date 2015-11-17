/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.utils;

import java.text.Normalizer;
import java.text.Normalizer.Form;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public class StringUtils {

    public static String normalize(String text) {
        return text == null ? null
                : Normalizer.normalize(text, Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
