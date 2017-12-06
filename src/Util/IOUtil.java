package Util;

import elements.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import topicModelling.Model;

public class IOUtil {

    private String inputDirectory;
    private String aspectKeywordsFile;
    private String stopwordFile;
    private String collocationsFile;
    private HashSet<Entity> entities;
    private HashMap<String,ArrayList<Review>> reviews;
    private String fileSeparator;
    public Set<String> stopwords;
    public Set<String> collocations;
    public HashMap<String, String> aspectWords;
    public HashMap<String, Integer> aspectMap = new HashMap<>();
    HashMap<String,Integer> wordMap = new HashMap<>();
    HashMap<Integer,String> InvwordMap = new HashMap<>();
    HashMap<Integer, String> revAspectMap = new HashMap<>();
    int noOfSentences = 0;
    public int A;
    public IOUtil() {
        fileSeparator = File.separator;
        getProperties();
        entities = new HashSet<>();
        reviews = new HashMap<>();
        stopwords = Helper.readStopwordFile(stopwordFile);
        collocations = Helper.readCollocationFile(collocationsFile);
        aspectWords = Helper.readAspectKeywordsFile(aspectKeywordsFile);
        createAspectMap();
        A = aspectMap.size();
        System.out.println("Aspects : "+A);
    }

    private void createAspectMap() {
        aspectMap.put("Service", 3);
        aspectMap.put("Food", 4);
        //aspectMap.put("Amenities", 5);
        aspectMap.put("Value", 0);
        aspectMap.put("Rooms", 1);
        aspectMap.put("Location", 2);
        
        revAspectMap.put(3,"Service");
        revAspectMap.put(4,"Food");
        //revAspectMap.put(5,"Amenities");
        revAspectMap.put(0,"Value");
        revAspectMap.put(1,"Rooms");
        revAspectMap.put(2,"Location");
    }

    public HashSet<Entity> getEntities() {
        return entities;
    }
    
    public ArrayList<Review> getReviews(String entId) {
        return reviews.get(entId);
    }

    public void getProperties() {
        Properties prop = new Properties();
        String propFileName = "config.properties";
        try {
            InputStream inputStream = new FileInputStream("resources" + fileSeparator + propFileName);
            prop.load(inputStream);
        } catch (IOException e) {
            System.out.println("Input directory not found");
            e.printStackTrace();
        }

        inputDirectory = prop.getProperty("input_directory");
        aspectKeywordsFile = prop.getProperty("aspect_keywords_file");
        stopwordFile = prop.getProperty("stopwords_file");
        collocationsFile = prop.getProperty("collocations_file");
    }

    public void readJSONFiles() {
        try {
            int noOfReviews = 0;
            System.out.println("Starting to read JSON files");
            File folder = new File(inputDirectory);
            for (File filePath : folder.listFiles()) {
                if (filePath.isFile() && filePath.getName().endsWith("json")) {
                    noOfReviews += readJSONfile(filePath.toString());
                }
            }
            System.out.println("Number Of Reviews: "+noOfReviews);
        } catch (Exception ex) {
            Logger.getLogger(IOUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Finished reading JSON files");
        //System.out.println("No of sentences: "+ noOfSentences);
        //System.out.println("Vocab size: "+ wordMap.size());
    }

    private int readJSONfile(String fileName) {
        int noOfReviews = 0;
        JSONParser parser = new JSONParser();
        try {

            Object obj = parser.parse(new FileReader(
                    fileName));

            JSONObject jsonObject = (JSONObject) obj;

            JSONObject hotel = (JSONObject) jsonObject.get("HotelInfo");
            JSONArray reviews = (JSONArray) jsonObject.get("Reviews");

            ArrayList<Review> hotelReviews = new ArrayList<>();

            Iterator<JSONObject> iterator = reviews.iterator();
            while (iterator.hasNext()) {
                JSONObject review = (JSONObject) iterator.next();
                Review r = initializeReviewObject(review);
                hotelReviews.add(r);
                noOfReviews++;
            }
            hotelReviews.trimToSize();
            HotelInfo hotelInfo = initializeHotelObject(hotel, hotelReviews);
            entities.add(hotelInfo);
            this.reviews.put(hotelInfo.getHotelID()+"", hotelReviews);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return noOfReviews;
    }

    public void readTxtFiles(char c) {
        try {
            System.out.println("Starting to read files");
            File folder = new File(inputDirectory);
            for (File filePath : folder.listFiles()) {
                if (filePath.isFile()) {
                    if (c == 'y') {
                        readYelpFile(filePath.toString());
                    }
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(IOUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Finished reading files");
    }

    public Review initializeReviewObject(JSONObject review) {
        JSONObject ratings = (JSONObject) review.get("Ratings");

        HashMap<Integer, Double> rating = new HashMap<Integer, Double>(7);
        if (ratings.get("Service") != null) {
            rating.put(aspectMap.get("Service"), Double.parseDouble((String) ratings.get("Service")));
        } else {
            rating.put(aspectMap.get("Service"), -1.0);
        }
        if (ratings.get("Value") != null) {
            rating.put(aspectMap.get("Value"), Double.parseDouble((String) ratings.get("Value")));
        } else {
            rating.put(aspectMap.get("Value"), -1.0);
        }
        if (ratings.get("Rooms") != null) {
            rating.put(aspectMap.get("Rooms"), Double.parseDouble((String) ratings.get("Rooms")));
        } else {
            rating.put(aspectMap.get("Rooms"), -1.0);
        }
        if (ratings.get("Location") != null) {
            rating.put(aspectMap.get("Location"), Double.parseDouble((String) ratings.get("Location")));
        } else {
            rating.put(aspectMap.get("Location"), -1.0);
        }

        String reviewId = (String) review.get("ReviewID");
        String author = (String) review.get("Author");
        String content = (String) review.get("Content");
        content = content.replace("[", "(").replace("]", ")");
        String date = (String) review.get("Date");
        Review r = new Review(reviewId, author, rating, content);
        r.setDate(date);
        return r;

    }

    private HotelInfo initializeHotelObject(JSONObject hotel, ArrayList<Review> hotelReviews) {
        String hotelName = (String) hotel.get("Name");
        String price = (String) hotel.get("Price");
        String address = (String) hotel.get("Address");
        int hotelID = Integer.parseInt((String) hotel.get("HotelID"));

        HotelInfo h = new HotelInfo(hotelName, price, address, hotelID,A, hotelReviews);
        return h;
    }
    
    private void readYelpFile(String fileName) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            String reviewID = line.split(":")[1].trim();
            line = br.readLine(); // reviewerid
            String reviewerID = line.split(":")[1].trim();
            line = br.readLine(); //rating
            line = br.readLine(); //restaurantName
            String restaurantName = line.split(":")[1].trim().toLowerCase();
            line = br.readLine(); //category
            line = br.readLine(); //content
            sb = sb.append(line);
            while (line != null) {
                line = br.readLine(); //content
                sb = sb.append(line);
            }
            String content = sb.toString().trim().replace("[", "(").replace("]", ")").toLowerCase();
            //content = Helper.replaceCollocations(content.toLowerCase(), collocations);
            Review r = new Review(reviewID, reviewerID, null, content);

            boolean flag = false;
            for (Entity e : entities) {
                if (e.getEntityName().equals(restaurantName)) {
                    ArrayList<Review> reviews = e.getReviews();
                    reviews.add(r);
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                ArrayList<Review> reviews = new ArrayList<>();
                reviews.add(r);
                Restaurant res = new Restaurant(restaurantName, entities.size(), reviews);
                entities.add(res);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public Model readModel(String folderName) {
        Properties prop = new Properties();
        String propFileName = "Params.properties";
        try {
            InputStream inputStream = new FileInputStream(folderName + fileSeparator + propFileName);
            prop.load(inputStream);
        } catch (IOException e) {
            System.out.println("Input directory not found");
            e.printStackTrace();
        }

        int aspect = 1;
        if(prop.containsKey("Aspect"))
            aspect = Integer.parseInt(prop.getProperty("Aspect"));
        
        String readTopics = prop.getProperty("Topics");
        int[] topics = new int[aspect];
        if(readTopics.contains(",")){
            String[] topic = readTopics.split(",");
            for(int t=0;t<topic.length; t++){
                topics[t] = Integer.parseInt(topic[t]);
            }
        }else{
            int topicsPerAspect = Integer.parseInt(readTopics);
            for(int t=0;t<aspect; t++){
                topics[t] = topicsPerAspect;
            }
        }
        int sentiment = 1;
        if(prop.containsKey("Sentiment"))
            sentiment = Integer.parseInt(prop.getProperty("Sentiment"));
        
        Model m = new Model(aspect, topics, sentiment);
        
        int[][] wordTopicDist = null;
        int[] wordsPerTopic = null;
        int W, numTopics;
        int w =0;
        String line;
        numTopics = 1; // background topic
        try {
            BufferedReader br = new BufferedReader(new FileReader(folderName+"wordTopicDist.txt"));
            line = br.readLine();
            W = Integer.parseInt(line);
            for(int a=0; a<aspect; a++){
                numTopics += topics[a]*sentiment;
            }
            
            int topicMask, topicBits;
            if (Integer.bitCount(numTopics) == 1) {
                // exact power of 2
                topicMask = numTopics - 1;
                topicBits = Integer.bitCount(topicMask);
            } else {
                // otherwise add an extra bit
                topicMask = Integer.highestOneBit(numTopics) * 2 - 1;
                topicBits = Integer.bitCount(topicMask);
            }
            
            wordTopicDist = new int[W][];
            wordsPerTopic = new int[numTopics];
            line = br.readLine();
            while (line != null && !line.trim().equals("")) {
                ArrayList<Integer> topicVal = new ArrayList<>();
                String[] fields = line.split("\t");
                String word = fields[0];
                wordMap.put(word, w);
                InvwordMap.put(w,word);
                for(int t=1; t< fields.length; t++){
                    int val = Integer.parseInt(fields[t]);
                    if(val>0){
                        int currTopic = t-1; 
                        int x;
                        x = val;
                        x = x << topicBits;
                        x = x | currTopic;
                        topicVal.add(x);
                        wordsPerTopic[t-1] += val;
                    }
                }
                wordTopicDist[w] = new int[topicVal.size()];
                for(int i=0; i<wordTopicDist[w].length; i++)
                    wordTopicDist[w][i] = topicVal.get(i);
                w++;
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        m.setInvwordMap(InvwordMap);
        m.setWordMap(wordMap);
        m.setWordTopicDist(wordTopicDist);
        m.setWordsPerTopic(wordsPerTopic);
        return m;
    }

    int[][] readTestDocs(String testDir) {
        int[][] testDocs = null;
        ArrayList<int[]> tokenSequences = new ArrayList<>();
        try {
            System.out.println("Starting to read test files");
            int newWords =0;
            File folder = new File(testDir);
            for (File filePath : folder.listFiles()) {
                ArrayList<String> tokenList = new ArrayList<>();
                BufferedReader br = new BufferedReader(new FileReader(filePath));
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                String reviewID = line.split(":")[1].trim();
                line = br.readLine(); // reviewerid
                String reviewerID = line.split(":")[1].trim();
                line = br.readLine(); //rating
                line = br.readLine(); //restaurantName
                String restaurantName = line.split(":")[1].trim().toLowerCase();
                line = br.readLine(); //category
                line = br.readLine(); //content
                sb = sb.append(line);
                while (line != null) {
                    line = br.readLine(); //content
                    sb = sb.append(line);
                }
                String content = sb.toString().trim().replace("[", "(").replace("]", ")").toLowerCase();
                content = Helper.normalizeSentence(content, stopwords);
                String[] rawTokens = content.split("\\s+|!|\\.");
                for (String word : rawTokens) {
                    if (!(word == null || word.equals("") || word.equals("-"))) {
                        tokenList.add(word);
                    }
                }
                String[] words = tokenList.toArray(new String[tokenList.size()]);
                int[] tokenSequence = new int[words.length];
                for(int i=0; i< words.length; i++){
                    String word = words[i];
                    int w;
                    if(wordMap.containsKey(word)){
                        w = wordMap.get(word);
                    }else{
                        w = wordMap.size()+newWords;
                        newWords++;
                    }
                    tokenSequence[i] = w;
                }
                tokenSequences.add(tokenSequence);
            }
            testDocs = new int[tokenSequences.size()][];
            for(int d=0; d<tokenSequences.size(); d++){
                testDocs[d] = tokenSequences.get(d);
            }

        } catch (Exception ex) {
            Logger.getLogger(IOUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Finished reading files");
   
        return testDocs;
    }
    
    public int[][] readTestJSONDocs(String testDir) {
        int[][] testDocs = null;
        ArrayList<int[]> tokenSequences = new ArrayList<>();
        try {
            System.out.println("Starting to read test files");
            int newWords =0;
            File folder = new File(testDir);
            JSONParser parser = new JSONParser();
            for (File filePath : folder.listFiles()) {
                Object obj = parser.parse(new FileReader(
                    filePath));

            JSONObject jsonObject = (JSONObject) obj;

            JSONObject hotel = (JSONObject) jsonObject.get("HotelInfo");
            JSONArray reviews = (JSONArray) jsonObject.get("Reviews");

            Iterator<JSONObject> iterator = reviews.iterator();
            while (iterator.hasNext()) {
                JSONObject review = (JSONObject) iterator.next();
                Review r = initializeReviewObject(review);
            
                ArrayList<String> tokenList = new ArrayList<>();
                String content = r.getContent().trim().replace("[", "(").replace("]", ")").toLowerCase();
                content = Helper.normalizeSentence(content, stopwords);
                String[] rawTokens = content.split("\\s+|!|\\.");
                for (String word : rawTokens) {
                    if (!(word == null || word.equals("") || word.equals("-"))) {
                        tokenList.add(word);
                    }
                }
                String[] words = tokenList.toArray(new String[tokenList.size()]);
                int[] tokenSequence = new int[words.length];
                for(int i=0; i< words.length; i++){
                    String word = words[i];
                    int w;
                    if(wordMap.containsKey(word)){
                        w = wordMap.get(word);
                    }else{
                        w = wordMap.size()+newWords;
                        newWords++;
                    }
                    tokenSequence[i] = w;
                }
                tokenSequences.add(tokenSequence);
            }
            }
            testDocs = new int[tokenSequences.size()][];
            for(int d=0; d<tokenSequences.size(); d++){
                testDocs[d] = tokenSequences.get(d);
            }
            System.out.println("No of Reviews: "+tokenSequences.size());

        } catch (Exception ex) {
            Logger.getLogger(IOUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Finished reading files");
   
        return testDocs;
    }
}
