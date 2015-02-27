/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.democracit.wordcloud.dba;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.scify.democracit.model.Comment;
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

    /**
     * Custom helper method
     *
     * @param toRestore
     * @param encoding
     * @return
     * @throws java.io.IOException
     */
    public List fromFileAsCommentList(String toRestore, String encoding) throws IOException {
        List lsRes = new ArrayList<>();
        File fFile = new File(toRestore);
        FileInputStream fstream = null;
        if (fFile.canRead()) {
            try {
                fstream = new FileInputStream(fFile);
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName(encoding)));
                JsonElement jsonElem = new JsonParser().parse(br);
                JsonArray array = jsonElem.getAsJsonArray();
                for (JsonElement eachSegStr : array) {
                    Gson gson = new Gson();
                    Comment eachSeg = gson.fromJson(eachSegStr, Comment.class);
                    lsRes.add(eachSeg);
                }
                return lsRes;
            } finally {
                if (fstream != null) {
                    fstream.close();
                }
            }
        } else {
            logger.warn("Could not read from file " + toRestore);
            return null;
        }
    }

}
