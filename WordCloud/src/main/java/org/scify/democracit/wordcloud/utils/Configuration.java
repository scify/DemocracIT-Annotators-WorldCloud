/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public class Configuration {

    private final Properties properties;

    public static final String FILE_SEP = System.getProperty("file.separator");

    public static final String UTF8 = "UTF-8";

    /**
     * Reads properties from a key-value paired file.
     *
     * @param configurationFileName the path to the file to read properties from
     */
    public Configuration(String configurationFileName) {
        File file = new File(configurationFileName);
        this.properties = new Properties();
        try {
            this.properties.load(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an empty properties file, to accept parameters at runtime
     */
    public Configuration() {
        this.properties = new Properties();
    }
//

    /**
     *
     * @return the path to the catalogue where the custom stopword files are
     * located
     */
    public String getStopWordsFilePath() {
        String fileName = properties.getProperty("stopWordsFileName", "stopwords");
        if (fileName.endsWith(FILE_SEP)) {
            return getWorkingDir() + fileName;
        } else {
            return getWorkingDir() + fileName + FILE_SEP;
        }

    }

    /**
     *
     * @return the path to the GR stopwords file.
     */
    public String getGRStopwordsFilePath() {
        return (getStopWordsFilePath() + properties.getProperty("GR_stopwords"));
    }

    /**
     *
     * @return the GR stopwords file
     */
    public File getGRStopwordsFile() {
        return new File(getStopWordsFilePath() + properties.getProperty("GR_stopwords"));
    }

    /**
     *
     * @return the directory where all files are stored and read from
     */
    public String getWorkingDir() {
        String sWorkingDir = properties.getProperty("workingDir");

        if (!sWorkingDir.endsWith(FILE_SEP)) {
            return sWorkingDir + FILE_SEP;
        } else {
            return sWorkingDir;
        }

    }

    /**
     * Manually sets 'working directory'
     *
     * @param sWorkingDir the path to set
     * @see {@link #getWorkingDir() }
     */
    public void setWorkingDir(String sWorkingDir) {
        properties.put("workingDir", sWorkingDir);
    }

    /**
     *
     * @return the query limit for the selects to use.
     */
    public int getQueryLimit() {
        return Integer.valueOf(properties.getProperty("queryLimit", "200"));
    }

    /**
     *
     * @return the charset that should be used by the application
     */
    public Charset getGlobalCharset() {
        return Charset.forName(UTF8);
    }

    /**
     *
     * @return the max amount of batch INSERTs/UPDATEs to be used in the
     * corresponding DB driver
     */
    public int getmaxStoreCount() {
        return Integer.valueOf(properties.getProperty("maxStoreCount", "300"));
    }

    /**
     *
     * @return the top N bi-grams to keep
     */
    public int getTopNBiGramsToStore() {
        return Integer.valueOf(properties.getProperty("topN_biGrams", "10"));
    }

    public String getModuleName() {
        return properties.getProperty("db_name", "WordCloud Extractor");
    }

    public int getMaxTermsToResponde() {
        return Integer.valueOf(properties.getProperty("max_terms", "200"));
    }

    public Object getResetCode() {
        return properties.getProperty("reset_code");
    }
}
