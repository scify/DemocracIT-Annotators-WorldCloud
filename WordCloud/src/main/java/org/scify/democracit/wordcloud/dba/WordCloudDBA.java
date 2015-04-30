/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.dba;

import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import javax.sql.DataSource;
import org.scify.democracit.demoutils.logging.ILogger;
import org.scify.democracit.wordcloud.model.TermResult;

/**
 *
 * @author George K. <gkiom@scify.org>
 */
public class WordCloudDBA {

    /**
     * The DataBase Access Resource.
     */
    protected DataSource dataSource;
    /**
     * The logger
     */
    protected ILogger logger;

    /**
     *
     * @param dataSource the DBA resource
     * @param logger the custom logger
     */
    public WordCloudDBA(DataSource dataSource, ILogger logger) {
        this.dataSource = dataSource;
        this.logger = logger;
    }

    /**
     * custom helper method
     *
     * @param sPath the path to store the {@link TermResult} item (full
     * filename)
     * @param trTerm the {@link TermResult} to store
     * @throws java.io.IOException
     */
    public void storeToJSON(String sPath, TermResult trTerm) throws IOException {
        File fPath = new File(sPath);
        if (fPath.canWrite()) {
            String fName = String.valueOf(trTerm.getID());
            Map<String, Float> termMap = trTerm.getTermFrequencyMap();
            // create json
            String jsonMap = new Gson().toJson(termMap, Map.class);
            // write to file.
            File fFile = new File((sPath.endsWith("/") ? sPath + fName : sPath + "/" + fName) + ".json");
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(fFile, false));
                bw.write(jsonMap);
            } finally {
                if (bw != null) {
                    bw.close();
                }
            }
        } else {
            logger.warn("Cannot write to " + sPath);
        }
    }

}
