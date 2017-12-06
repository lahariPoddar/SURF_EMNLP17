/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Util;

import elements.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.json.simple.parser.JSONParser;
import org.json.simple.*;
/**
 *
 * @author Lahari
 * @created on Mar 9, 2015
 */
public class Helper {

    public static Set<String> readStopwordFile(String fileName) {
        Set<String> stopwords = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                stopwords.add(line.trim().toLowerCase());

                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println("Stopword file not found!");
        }
        return stopwords;
    }
    
    public static HashMap<String,String> readPolarityFile(String fileName) {
        HashMap<String,String> polarityWords = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            String line = br.readLine();
            String word,polarity;
            while (line != null) {
                String[] parts = line.trim().split("\\s+");
                if(parts.length==6&& parts[1].equals("type=strongsubj") &&parts[2].contains("word1=") && parts[5].contains("priorpolarity")){
                    word = parts[2].split("=")[1];
                    polarity = parts[5].split("=")[1];
                    polarityWords.put(word, polarity);
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println("Polarity file not found!");
        }
        return polarityWords;
    }
    
    public static HashMap<String,String> readAspectKeywordsFile(String aspectKeywordFile) {
        HashMap<String,String> aspectKeywords = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(aspectKeywordFile));
            String line = br.readLine();
            while (line != null) {
                String[] fields = line.split("=");
                String[] wordSets = fields[1].split(":");
                HashSet<String> words = new HashSet<String>(Arrays.asList(wordSets[0].split(",")));
                for(String word:words)
                    aspectKeywords.put(word, fields[0]);
                if(wordSets.length>1){
                    words = new HashSet<String>(Arrays.asList(wordSets[1].split(",")));
                    for(String word:words)
                        aspectKeywords.put(word, fields[0]+"_"+0);
                    words = new HashSet<String>(Arrays.asList(wordSets[2].split(",")));
                    for(String word:words)
                        aspectKeywords.put(word, fields[0]+"_"+1);
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println("Aspect keyword file not found!");
        }
        return aspectKeywords;
    }
    
        public static Set<String> readCollocationFile(String fileName) {
        Set<String> collocations = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                line = line.split(":")[0];
                collocations.add(line.trim().toLowerCase());

                line = br.readLine();
            }
        } catch (IOException e) {
            System.out.println("Collocation file not found!");
        }
        return collocations;
    }

    public static void writeReviewsToFile(ArrayList<Review> reviews) {
        FileWriter writer = null;
        try {
            writer = new FileWriter("TM\\reviewContents_all.txt", true);
            Iterator<Review> it = reviews.iterator();
            while (it.hasNext()) {
                writer.write(it.next().getContent());
            }
            writer.write("\n");
            writer.flush();
            writer.close();
        } catch (Exception ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void writeReviewWIthAspectsToFile(Review review, String outputFileName, HashMap<String, Integer> topicMapping) {
        FileWriter writer = null;

        try {
            writer = new FileWriter(outputFileName, true);
            Iterator<String> it1 = review.getAspectSentences().keySet().iterator();
            if (review.getAspectSentences().keySet().size() > 0) {
                writer.write("[");
                boolean isFirst = true;
                while (it1.hasNext()) {
                    if (!isFirst) {
                        writer.write(" ");
                    }
                    writer.write(topicMapping.get(it1.next()).toString());
                    isFirst = false;
                }
                writer.write("]");
            }
            writer.write(review.getContent());
            writer.write("\n");
            writer.flush();
        } catch (Exception ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void createGZIP(String fileName) {
        byte[] buffer = new byte[1024];

        try {

            FileOutputStream fileOutputStream = new FileOutputStream(fileName.replace(".txt", ".gz"));

            GZIPOutputStream gzipOuputStream = new GZIPOutputStream(fileOutputStream);

            FileInputStream fileInput = new FileInputStream(fileName);

            int bytes_read;

            while ((bytes_read = fileInput.read(buffer)) > 0) {
                gzipOuputStream.write(buffer, 0, bytes_read);
            }

            fileInput.close();

            gzipOuputStream.finish();
            gzipOuputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void normalize(String inputFile, String outputFile, Set<String> stopwords, String fileSeparator) {

        FileWriter writer = null;
        //MaxentTagger tagger = new MaxentTagger("libs"+fileSeparator+"tagger"+fileSeparator+"english-left3words-distsim.tagger");
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            String line = br.readLine();

            writer = new FileWriter(outputFile);
            while (line != null) {

                String labels = "";
                String content = line;
                if (line.contains("]") && line.contains("[")) {
                    try {
                        labels = line.substring(0, line.indexOf("]") + 1);
                        content = line.substring(line.indexOf("]") + 2);
                    } catch (Exception e) {
                        System.out.println("Exception string: " + line);
                    }
                }
                // String taggedLine = tagger.tagString(content);
                // String strippedLine = retainNouns(taggedLine);
                writer.write((labels + normalizeSentence(content, stopwords)).trim());
                //writer.write(normalizeSentence(line, collocations));
                writer.write("\n");
                line = br.readLine();
            }
        } catch (IOException e) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static String normalizeSentence(String line, Set<String> stopwords) {

        StringBuilder lineWithoutStopword = new StringBuilder();
        String[] words = line.toLowerCase().replace(',', ' ').replace('.', ' ').trim().split(" ");
        for (String word : words) {
            if (word.matches("[a-zA-Z\\-']+") && !stopwords.contains(word)) {
                lineWithoutStopword.append(word + " ");
            }
        }
        String normalizedLine = lineWithoutStopword.toString().trim();
        normalizedLine = normalizedLine.replaceAll(" not ", " not-");
        return normalizedLine;
    }

   public static Map sortByValue(Map unsortMap) {
        List list = new LinkedList(unsortMap.entrySet());

        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue()) * -1;
            }
        });

        HashMap sortedMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }
    public static void gunzipIt(String fileName) {

        byte[] buffer = new byte[1024];

        try {

            GZIPInputStream gzis
                    = new GZIPInputStream(new FileInputStream(fileName));

            FileOutputStream out
                    = new FileOutputStream(fileName.replace(".gz", ""));

            int len;
            while ((len = gzis.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }

            gzis.close();
            out.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    static void printReviewContent(Review review) {
        FileWriter writer = null;

        try {
            writer = new FileWriter("output/reviewContents.txt", true);
            writer.write(review.getContent());
            writer.write("\n");
            writer.flush();
        } catch (Exception ex) {
            Logger.getLogger(Helper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

        static void writeAspectwords(Map aspectWords, String fileName) {
        FileWriter writer = null;

        try {
            writer = new FileWriter(fileName);
            Iterator<String> itr1 = aspectWords.keySet().iterator();
            while (itr1.hasNext()) {
                String aspect = itr1.next();
                writer.write("\n" + aspect + "=");
                Set<String> words = (Set) aspectWords.get(aspect);
                for(String word: words) {
                    writer.write(word + ",");
                }
            }
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
        
    static void printHashMap(Map mapToPrint, String fileName){
                FileWriter writer = null;
        try {
            writer = new FileWriter(fileName);
            Iterator<String> itr1 = mapToPrint.keySet().iterator();
            while (itr1.hasNext()) {
                String word = itr1.next();
                writer.write("\n" + word + ":"+mapToPrint.get(word));
            }
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static void writeTopwords(Map topWords, String fileName) {
        FileWriter writer = null;

        try {
            writer = new FileWriter(fileName);
            Iterator<String> itr1 = topWords.keySet().iterator();
            while (itr1.hasNext()) {
                String aspect = itr1.next();
                writer.write("\n" + aspect + "---> ");
                Map aspectWords = (Map) topWords.get(aspect);
                Iterator<String> itr2 = aspectWords.keySet().iterator();
                while (itr2.hasNext()) {
                    String word = itr2.next();
                    writer.write(word + ":" + aspectWords.get(word) + ";");
                }
            }
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String replaceCollocations(String content, Set<String> collocations) {
        for(String collocation:collocations){
            content = content.replaceAll(collocation.replace("-", " "), collocation);
        }
        return content;
    }

    public static void writeSentenceClusters(HotelInfo hotel, Vector<List> clusters) {
        try{
        File outputDir = new File("output" + File.separator+"clusters");
        if(outputDir.exists() || outputDir.mkdir()){
                FileWriter writer = new FileWriter(outputDir+File.separator+hotel.getHotelID()+".clusters");
                JSONArray clusterArray = new JSONArray();
                int count =0;
                for(List<Sentence> cluster: clusters){
                    JSONObject clusterObject = new JSONObject();
                    JSONArray sentenceArray = new JSONArray();
                    for(Sentence sent : cluster){
                        JSONObject sentenceObject = new JSONObject();
                        sentenceObject.put("content", sent.getSent());
                        sentenceArray.add(sentenceObject);
                    }
                   clusterObject.put("clusterID", count);
                   clusterObject.put("sentences", sentenceArray);
                   clusterArray.add(clusterObject);
                   count++;
                }
                writer.write(clusterArray.toJSONString());
                writer.flush();
            
        }
        }catch(Exception e){
            e.printStackTrace();
        }
                
    }
}
