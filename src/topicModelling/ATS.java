package topicModelling;

import Util.Pair;
import Util.Helper;
import elements.*;
import java.io.*;
import java.util.*;

public class ATS {

    public HashMap<String, Integer> wordMap;
    public HashMap<Integer, String> wordMapInv;

    public HashMap<Integer, Sentence> docs;
    public HashMap<Integer, Integer> docLength;

    public int[] nW;
    public int n0;

    public int[][] nDA;
    public int[][] nDL1;

    public int[][][] nDAT;
    public int[][][][] nDAST;
    public int[][][] nAST;
    public int[][][][] nASTW;
    public int[][][] nATW;

    public int topWords;

    public int D;
    public int W;
    public int A;
    public int T;
    public int S;

    public Double alpha10;
    public Double alpha11;
    public Double alpha3;
    public Double alpha3Norm;

    public Double alpha4;
    public Double alpha4Norm;

    public Double[] alpha5;
    public Double alpha5Norm;

    public Double alpha6;
    public Double alpha6Norm;
    public Double labelPrior;

    public int wordThreshold = 10;
    public int checkCount;

    public HashMap<String, String> aspectWords;

    public ATS(int t, int aspect, Double a0, Double a1, Double b0, Double b1, Double c, Double d, Double[] e,
            Double f, Double lp) {

        alpha10 = a0;
        alpha11 = a1;
        alpha3 = c;
        alpha4 = d;
        alpha5 = e;
        alpha6 = f;

        T = t;
        A = aspect;
        S = e.length;
        topWords = 20;

        labelPrior = lp;
        if (labelPrior == 0) {
            labelPrior = b0;
        }
    }

    public void initialize(HashMap<String, String> aspectWords) {
        System.out.println("Initializing...");

        alpha6Norm = W * alpha6;
        alpha3Norm = A * alpha3;
        alpha4Norm = 0.0;
        alpha5Norm = 0.0;
        for (int s = 0; s < S; s++) {
            alpha5Norm += alpha5[s];
        }
        HashMap<String, String> polarityWords = Helper.readPolarityFile("data" + File.separator + "polarity.txt");
        this.aspectWords = aspectWords;

        Random r = new Random(100);

        nDA = new int[D][A];
        nDL1 = new int[D][2];
        nW = new int[W];
        n0 = 0;
        nDAT = new int[D][A][T];
        nDAST = new int[D][A][S][T];
        nAST = new int[A][S][T];
        nASTW = new int[A][S][T][W];

        for (int d = 0; d < D; d++) {
            Sentence sent = docs.get(d);
            int[] words = sent.getTokens();
            int[] aspects = new int[words.length];
            int[] topics = new int[words.length];
            int[] sentiments = new int[words.length];
            int[] levels = new int[words.length];

            for (int n = 0; n < words.length; n++) {
                int w = words[n];

                int l1 = //r.nextInt(2);		// select random l1 value in {0,1}
                        initializeL1(w, aspectWords, polarityWords);
                levels[n] = l1;
                nDL1[d][l1] += 1;
                int a = initializeAspect(w, aspectWords);//r.nextInt(A);		// select random i value in {0...A-1}
                aspects[n] = a;
                int t = r.nextInt(T);		// select random a value in {0...T-1}
                topics[n] = t;
                int s = initializeSentiment(w, polarityWords);
                sentiments[n] = s;
                if (l1 == 0) {
                    nW[w] += 1;
                    n0 += 1;
                } else {
                    nDA[d][a] += 1;
                    nDAT[d][a][t] += 1;
                    nDAST[d][a][s][t] += 1;
                    nAST[a][s][t] += 1;
                    nASTW[a][s][t][w] += 1;
                }
            }
            sent.setLevels(levels);
            sent.setAspects(aspects);
            sent.setTopics(topics);
            sent.setSentiments(sentiments);
        }
    }

    public int initializeL1(int w, HashMap<String, String> aspectWord, HashMap<String, String> polarityWord) {
        String word = wordMapInv.get(w);
        Random r = new Random(100);
        int l1 = r.nextInt(2);
        if (aspectWord.containsKey(word) || polarityWord.containsKey(word)) {
            l1 = 1;
        }
        return l1;
    }

    public int initializeSentiment(int w, HashMap<String, String> polarityWord) {
        String word = wordMapInv.get(w);
        Random r = new Random(100);
        int s = r.nextInt(S);
        if (word.contains("not-")) {
            s = 1;
        } else if (aspectWords.containsKey(word)) {
            String[] fields = aspectWords.get(word).split("_");
            if (fields.length > 1) {
                s = Integer.parseInt(fields[1]);
            } else {
                s = 2;
            }
        } else if (polarityWord.containsKey(word)) {
            String polarity = polarityWord.get(word);
            if (polarity.equals("negative")) {
                s = 1;
            } else if (polarity.equals("positive")) {
                s = 0;
            } else {
                s = 2;
            }
        }
        return s;
    }

    public int initializeAspect(int w, HashMap<String, String> aspectWord) {
        String word = wordMapInv.get(w);
        Random r = new Random(100);
        int a = r.nextInt(A);
        if (aspectWord.containsKey(word)) {
            String[] fields = aspectWord.get(word).split("_");
            a = Integer.parseInt(fields[0]);
        }
        return a;
    }

    public void doSampling() {
        for (int d = 0; d < D; d++) {
            sample(d, docs.get(d));
        }
    }

    public void sample(int d, Sentence sent) {

        int[] words = sent.getTokens();
        int[] aspects = sent.getAspects();
        int[] topics = sent.getTopics();
        int[] sentiments = sent.getSentiments();
        int[] levels = sent.getLevels();

        for (int n = 0; n < words.length; n++) {
            int w = words[n];
            int a = aspects[n];
            int t = topics[n];
            int s = sentiments[n];
            int l1 = levels[n];
            if (l1 == 0) {
                nW[w] -= 1;
                n0 -= 1;
            } else {
                nDA[d][a] -= 1;
                nDAT[d][a][t] -= 1;
                nDAST[d][a][s][t] -= 1;
                nAST[a][s][t] -= 1;
                nASTW[a][s][t][w] -= 1;
            }
            
            a = sampleNewAspect(d, w, l1, s, t);
            t = sampleNewTopic(d, w, l1, a, s);
            s = sampleNewSentiment(d, w, l1, a, t);
            l1 = sampleNewLevel(d, w, a, s, t);
            if (l1 == 0) {
                nW[w] += 1;
                n0 += 1;
            } else {
                // increment counts
                nDA[d][a] += 1;
                nDAT[d][a][t] += 1;
                nDAST[d][a][s][t] += 1;
                nAST[a][s][t] += 1;
                nASTW[a][s][t][w] += 1;
            }
            // set new assignments
            levels[n] = l1;
            aspects[n] = a;
            topics[n] = t;
            sentiments[n] = s;

        }
        sent.setLevels(levels);
        sent.setAspects(aspects);
        sent.setTopics(topics);
        sent.setSentiments(sentiments);
    }

    public int sampleNewLevel(int d, int w, int a, int s, int t) {
        int l = 0;
        Double pTotal = 0.0;
        Double[] p = new Double[2];
        // l1 = 0
        p[0] = (nDL1[d][0] + alpha10)
                * (nW[w] + alpha6) / (n0 + alpha6Norm);

        // l1 = 1
        p[1] = (nDL1[d][1] + alpha11)
                * (nASTW[a][s][t][w] + alpha6) / (nAST[a][s][t] + alpha6Norm);

        pTotal = p[0] + p[1];

        Random r = new Random();
        Double u = r.nextDouble() * pTotal;

        Double v = 0.0;
        for (int i = 0; i < 2; i++) {
            v += p[i];
            if (v > u) {
                l = i;
                break;
            }
        }
        return l;
    }

    public int sampleNewAspect(int d, int w, int l1, int s, int t) {
        Double pTotal = 0.0;
        Double[] p = new Double[A];
        Double prior, likelihood;
        if (l1 == 0) {
            for (int i = 0; i < A; i++) {
                prior = (nDA[d][i] + alpha3) / (docLength.get(d) + alpha3Norm);
                p[i] = prior;
                pTotal += p[i];
            }
        } else {
            for (int i = 0; i < A; i++) {
                prior = (nDA[d][i] + alpha3) / (docLength.get(d) + alpha3Norm);
                likelihood = (nASTW[i][s][t][w] + alpha6) / (nAST[i][s][t] + alpha6Norm);
                p[i] = prior * likelihood;
                pTotal += p[i];
            }
        }
        Random r = new Random();
        Double u = r.nextDouble() * pTotal;

        int a = 0;
        Double v = 0.0;
        for (int i = 0; i < A; i++) {
            v += p[i];

            if (v > u) {
                a = i;
                break;
            }
        }
        return a;
    }

    private int sampleNewTopic(int d, int w, int l1, int a, int s) {
        Double pTotal = 0.0;
        Double[] p = new Double[T];
        Double prior, likelihood;
        if (l1 == 0) {
            for (int i = 0; i < T; i++) {
                prior = (nDAT[d][a][i] + alpha4) / (nDA[d][a] + alpha4Norm);
                p[i] = prior;
                pTotal += p[i];
            }
        } else {
        for (int i = 0; i < T; i++) {
            p[i] = (nDAT[d][a][i] + alpha4) / (nDA[d][a] + alpha4Norm)
                    * (nASTW[a][s][i][w] + alpha6) / (nAST[a][s][i] + alpha6Norm);
            pTotal += p[i];
        }
        }
        Random r = new Random();
        Double u = r.nextDouble() * pTotal;

        int t = 0;
        Double v = 0.0;
        for (int i = 0; i < T; i++) {
            v += p[i];

            if (v > u) {
                t = i;
                break;
            }
        }
        return t;
    }

    private int sampleNewSentiment(int d, int w, int l1, int a, int t) {
        Double pTotal = 0.0;
        Double[] p = new Double[S];
        double prior, likelihood;
        if (l1 == 0) {
            for (int i = 0; i < S; i++) {
                prior = (nDAST[d][a][i][t] + alpha5[i]) / (nDAT[d][a][t] + alpha5Norm);
                p[i] = prior;
                pTotal += p[i];
            }
        } else {
        for (int i = 0; i < S; i++) {
            prior = (nDAST[d][a][i][t] + alpha5[i]) / (nDAT[d][a][t] + alpha5Norm);
            likelihood = (nASTW[a][i][t][w] + alpha6) / (nAST[a][i][t] + alpha6Norm);
            p[i] = prior
                    * likelihood;
            pTotal += p[i];
        }
        }
        Random r = new Random();
        Double u = r.nextDouble() * pTotal;

        int s = 0;
        Double v = 0.0;
        for (int i = 0; i < S; i++) {
            v += p[i];
            if (v > u) {
                s = i;
                break;
            }
        }
        return s;
    }

    public void readDocs(HashSet<Entity> entities, Set<String> stopwords, Set<String> collocations) {
        System.out.println("Reading input... Number of entities: " + entities.size());
        wordMap = new HashMap<String, Integer>();
        wordMapInv = new HashMap<Integer, String>();

        docs = new HashMap<>();
        docLength = new HashMap<>();

        int d = 0;
        for (Entity e : entities) {
            ArrayList<Review> reviews = e.getReviews();
            for (Review r : reviews) {
                ArrayList<Sentence> reviewSentences = new ArrayList<>();
                String[] contentSentences = r.getContent().split("[\\.\\?!]|but|however");
                int s = 0;
                for (String contentSentence : contentSentences) {
                    String line = contentSentence.replace(',', ' ').replace('.', ' ');
                    line = Helper.normalizeSentence(line.toLowerCase(), stopwords);
                    //line = Helper.replaceCollocations(line, collocations);
                    String[] rawTokens = line.split("\\s+|!|\\.");
                    ArrayList<String> tokenList = new ArrayList<>();
                    for (String word : rawTokens) {
                        if (!(word == null || word.equals("") || word.equals("-"))) {
                            tokenList.add(word);
                        }
                    }
                    String[] words = tokenList.toArray(new String[tokenList.size()]);

                    int N = words.length;
                    int[] tokens = new int[N];
                    for (int n = 0; n < N; n++) {
                        String word = words[n];
                        int key = wordMap.size();
                        if (!wordMap.containsKey(word)) {
                            wordMap.put(word, key);
                            wordMapInv.put(key, word);
                        } else {
                            key = ((Integer) wordMap.get(word)).intValue();
                        }
                        tokens[n] = key;
                    }
                    if (tokens.length > 2) {
                        String sentId = e.getEntityID()+"_"+r.getReviewId()+"_"+s;
                        Sentence sent = new Sentence(contentSentence, tokens, words);
                        sent.setSentId(sentId);
                        reviewSentences.add(sent);
                        docs.put(d, sent);
                        docLength.put(d, sent.getNwords());
                        d++;
                    }
                    s++;
                }
                r.setSentences(reviewSentences);
            }

        }
        D = d;
        W = wordMap.size();
        System.out.println(d + " documents");
        System.out.println(W + " word types");
    }

    public void setProbabilities(HashSet<Entity> entities) {
        for (Entity e : entities) {
            ArrayList<Review> reviews = e.getReviews();
            for (Review r : reviews) {
                ArrayList<Sentence> sentences = r.getSentences();
                for (Sentence sent : sentences) {
                    int[] levels = sent.getLevels();
                    int[] words = sent.getTokens();
                    int[] topics = sent.getTopics();
                    int[] sentiments = sent.getSentiments();
                    int[] aspects = sent.getAspects();
                    Double[] prob = new Double[sent.getNwords()];

                    for (int n = 0; n < words.length; n++) {
                        int w = words[n];
                        int a = aspects[n];
                        int t = topics[n];
                        int s = sentiments[n];
                        prob[n] = (double) nASTW[a][s][t][w] / nAST[a][s][t];
                    }
                    sent.setProbabilities(prob);
                    initializeAspectProbabilityArray(sent);
                }
            }
        }
    }

    public void initializeAspectProbabilityArray(Sentence sent) {
        Double[] p = new Double[A];
        Double totalProb = 0.0;
        Arrays.fill(p, 0.0);
        for (int i = 0; i < sent.getNwords(); i++) {
            int l = sent.getLevels()[i];
            if (l == 0) {
                continue;
            }
            int a = sent.getAspects()[i];
            double prob = sent.getProbabilities()[i];
            p[a] += prob;
            totalProb += prob;
        }
        for (int i = 0; i < p.length; i++) {
            p[i] = p[i] / totalProb;
        }
        sent.setDepth(p);
    }
    
    public boolean writeTopwords(String filename) {
        Double probThreshold = 0.01;
        try {
            FileWriter fw = new FileWriter(filename);
            BufferedWriter writer = new BufferedWriter(fw);

            if (topWords > W) {
                topWords = W;
            }
            writer.write("Background Words :\n");

            ArrayList<Pair> wordsProbsList = new ArrayList<Pair>();
            for (int w = 0; w < W; w++) {
                Double prob = (double) nW[w] / n0;
                if (prob < probThreshold) {
                    continue;
                }
                Pair p = new Pair(w, prob, false);
                wordsProbsList.add(p);
            }
            if (wordsProbsList.size() != 0) {
                Collections.sort(wordsProbsList);
                int length = Math.min(topWords, wordsProbsList.size());
                for (int i = 0; i < length; i++) {
                    if (wordMapInv.containsKey((Integer) wordsProbsList.get(i).first)) {
                        String word = wordMapInv.get((Integer) wordsProbsList.get(i).first);
                        writer.write("\t" + word + "\t" + wordsProbsList.get(i).second + "\n");
                    }
                }
            }
            for (int a = 0; a < A; a++) {
                writer.write("Aspect " + a + ":\n");
                for (int t = 0; t < T; t++) {
                    writer.write("Topic " + t + ":\n");
                    for (int s = 0; s < S; s++) {
                        wordsProbsList = new ArrayList<Pair>();
                        writer.write("Sentiment " + s + ":\n");
                        for (int w = 0; w < W; w++) {
                            Double prob = getSentimentWordProb(a, t, s, w);
                            if (prob < probThreshold) {
                                continue;
                            }
                            Pair p = new Pair(w, prob, false);
                            wordsProbsList.add(p);
                        }
                        if (wordsProbsList.size() == 0) {
                            continue;
                        }
                        Collections.sort(wordsProbsList);
                        int length = Math.min(topWords, wordsProbsList.size());
                        for (int i = 0; i < length; i++) {
                            if (wordMapInv.containsKey((Integer) wordsProbsList.get(i).first)) {
                                String word = wordMapInv.get((Integer) wordsProbsList.get(i).first);
                                writer.write("\t" + word + "\t" + wordsProbsList.get(i).second + "\n");
                            }
                        }

                    }
                }
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            System.out.println("Error while saving model topwords: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Double getSentimentWordProb(int a, int t, int s, int w) {
        try {
            if (nASTW[a][s][t][w] < wordThreshold) {
                return 0.0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Double prob = (Double) (nASTW[a][s][t][w] + alpha6) / (nAST[a][s][t] + alpha6Norm);
        return prob;
    }

 public Double getWordProb(int a, int t, int w, int atCount) {
     
     int wordCount = 0;
     for(int s=0;s<S;s++){
         wordCount += nASTW[a][s][t][w];
     }
            if (wordCount < wordThreshold) {
                return 0.0;
            }
        Double prob = (Double) (wordCount + alpha6) / (atCount + alpha6Norm);
        return prob;
    }
 
public void saveModel(String folderPath) {
        try {
            FileWriter fw1 = new FileWriter(folderPath+File.separator + "wordTopicDist.txt");
            BufferedWriter writer = new BufferedWriter(fw1);
            writer.write(W+"\n");
            Iterator<Integer> it = wordMapInv.keySet().iterator();
            while (it.hasNext()) {
                int w = it.next();
                writer.write(wordMapInv.get(w)+"\t");
                int bgCount = nW[w];
                writer.write(bgCount + "\t");
                for (int a = 0; a < A; a++) {
                    for (int t = 0; t < T; t++) {
                        for (int s = 0; s < S; s++) {
                            writer.write(nASTW[a][s][t][w]+"\t");
                        }
                    }
                }
                writer.write("\n");
            }
            writer.flush();
            writer.close();
            fw1 = new FileWriter(folderPath+File.separator + "Params.properties");
            writer = new BufferedWriter(fw1);
            writer.write("Aspect = "+A);
            writer.write("\n");
            writer.write("Topics = "+T);
            writer.write("\n");
            writer.write("Sentiment = "+S);
            writer.flush();
            writer.close();
        } catch (Exception e) {

        }
    }


}
