package topicModelling;

import Util.Pair;
import elements.*;
import Util.Helper;
import java.io.*;
import java.util.*;

public class ATSNonParam {

    public HashMap<String, Integer> wordMap;
    public HashMap<Integer, String> wordMapInv;

    public HashMap<Sentence, Integer> docs;
    public HashMap<Integer, Integer> docLength;

    public HashMap<String, Integer> authors;
    public HashMap<Integer, Integer> docAuthors;

    public HashMap<String, Integer> entities;
    public HashMap<Integer, Integer> docEntities;

    public HashMap<Integer, ArrayList<Sentence>> collections;

    public int[] nW;
    public int n0;

    public int[][] nCA;
    public int[][] nDA;
    public int[][] nDL1;

    public int[][][] nDAT;
    public int[][][][] nDAST;
    public int[][][] nAST;
    public int[][][][] nASTW;
    public int[][][] nATW;

    public int[][][][] nUATS;
    public int[][][] nUAT;

    public int[][][][] nEATS;
    public int[][][] nEAT;

    public int topWords;

    public int C;
    public int D;
    public int W;
    public int A;
    public int T;
    public int S;
    public int U;
    public int E;

    public Double alpha10;
    public Double alpha11;
    public Double alpha3;
    public Double alpha3Norm;

    public Double topicConcentration;

    public Double[] alpha5;
    public Double alpha5Norm;

    public Double alpha6;
    public Double alpha6Norm;
    public Double labelPrior;

    public int wordThreshold = 10;
    public int checkCount;

    public HashMap<String, String> aspectWords;
    private Double aspectThreshold;

    public ATSNonParam(int t, int aspect, Double a0, Double a1, Double b0, Double b1, Double c, Double d, Double[] e,
            Double f, Double lp) {

        alpha10 = a0;
        alpha11 = a1;
        alpha3 = c;
        topicConcentration = d;
        alpha5 = e;
        alpha6 = f;

        T = t;
        A = aspect;
        S = e.length;
        topWords = 20;

        aspectThreshold = 0.003;

        labelPrior = lp;
        if (labelPrior == 0) {
            labelPrior = b0;
        }
    }

    public void initialize(HashMap<String, String> aspectWords) {
        System.out.println("Initializing...");

        alpha6Norm = W * alpha6;
        alpha3Norm = A * alpha3;
        alpha5Norm = 0.0;
        for (int s = 0; s < S; s++) {
            alpha5Norm += alpha5[s];
        }
        HashMap<String, String> polarityWords = Helper.readPolarityFile("data" + File.separator + "polarity.txt");
        this.aspectWords = aspectWords;

        Random r = new Random();

        nCA = new int[C][A];
        nDA = new int[D][A];
        nDL1 = new int[D][2];
        nW = new int[W];
        n0 = 0;
        nDAT = new int[D][A][T];
        nDAST = new int[D][A][S][T];
        nAST = new int[A][S][T];
        nASTW = new int[A][S][T][W];
        nUAT = new int[U][A][T];
        nUATS = new int[U][A][T][S];

        nEAT = new int[E][A][T];
        nEATS = new int[E][A][T][S];

        for (int c = 0; c < C; c++) {
            ArrayList<Sentence> sentences = collections.get(c);
            for (Sentence sent : sentences) {
                boolean firstTime = true;
                int maxT = 1;
                int d = docs.get(sent);
                int u = docAuthors.get(d);
                int e = docEntities.get(d);
                int[] words = sent.getTokens();
                //int[] aspects = new int[words.length];
                int[] topics = new int[words.length];
                int[] sentiments = new int[words.length];
                int[] levels = new int[words.length];

                int a = initializeAspect(sent.getSent(), aspectWords);//r.nextInt(A);		// select random i value in {0...A-1}
                sent.setAspect(a);
                nCA[c][a] +=1;
                for (int n = 0; n < words.length; n++) {
                    int w = words[n];

                    int l1 = //r.nextInt(2);		// select random l1 value in {0,1}
                            initializeL1(w, aspectWords, polarityWords);
                    levels[n] = l1;
                    nDL1[d][l1] += 1;
                    
                    for (int t1 = 0; t1 < T; t1++) {
                        if (nDAT[d][a][t1] == 0) {
                            break;
                        }
                        maxT = t1;
                    }
                    int t = r.nextInt(maxT+1);		// select random a value in {0...maxT}
                    if(firstTime)
                        t =0;
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
                        nUAT[u][a][t] += 1;
                        nUATS[u][a][t][s] += 1;
                        nEAT[e][a][t] += 1;
                        nEATS[e][a][t][s] += 1;
                    }
                    firstTime = false;
                }
                sent.setLevels(levels);
                //sent.setAspects(aspects);
                sent.setTopics(topics);
                sent.setSentiments(sentiments);
            }
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

    /**
     * return the aspect max of the words belong to
     *
     * @param sent
     * @param aspectWord
     * @return
     */
    public int initializeAspect(String sent, HashMap<String, String> aspectWord) {
        Random r = new Random();
        int a = r.nextInt(A);
        int[] counts = new int[A];
        String[] words = sent.split("\\s+");
        for (String word : words) {
            if (aspectWord.containsKey(word)) {
                String[] fields = aspectWord.get(word).split("_");
                int i = Integer.parseInt(fields[0]);
                counts[i]++;
            }
        }
        int max = 0;
        int index = -1;
        for (int i = 0; i < A; i++) {
            if (counts[i] > max) {
                max = counts[i];
                index = i;
            }
        }
        if (index == -1) {
            return a;
        } else {
            return index;
        }
    }

    public void doSampling(boolean flag) {
        for (int c = 0; c < C; c++) {
            sample(c, flag);
        }
    }

    public void sample(int c, boolean flag) {
        ArrayList<Sentence> sentences = collections.get(c);
        ArrayList<Integer> visitedAspects = new ArrayList<>();
        int lastAspect = -1;
        for (Sentence sent : sentences) {
            int d = docs.get(sent);
            int u = docAuthors.get(d);
            int e = docEntities.get(d);
            int[] words = sent.getTokens();
            int a = sent.getAspect();
            //int[] aspects = sent.getAspects();
            int[] topics = sent.getTopics();
            int[] sentiments = sent.getSentiments();
            int[] levels = sent.getLevels();

            int newA = sampleNewAspectForSentence(c,d, sent, visitedAspects,lastAspect);
            nCA[c][a] -=1;
            for (int n = 0; n < words.length; n++) {
                int w = words[n];
                //int a = aspects[n];
                int t = topics[n];
                int s = sentiments[n];
                int l1 = levels[n];
                String word = wordMapInv.get(w);

                if (l1 == 0) {
                    nW[w] -= 1;
                    n0 -= 1;
                } else {
                    nDA[d][a] -= 1;
                    nDAT[d][a][t] -= 1;
                    nDAST[d][a][s][t] -= 1;
                    nAST[a][s][t] -= 1;
                    nASTW[a][s][t][w] -= 1;
                    nUAT[u][a][t] -= 1;
                    nUATS[u][a][t][s] -= 1;
                    nEAT[e][a][t] -= 1;
                    nEATS[e][a][t][s] -= 1;
                }

                //a = sampleNewAspect(d, w, l1, s, t, visitedAspects);
                t = sampleNewTopic(d, w, l1, newA, s);
                s = sampleNewSentiment(e, u, d, w, l1, newA, t);
                l1 = sampleNewLevel(d, w, newA, s, t);
                if (l1 == 0) {
                    nW[w] += 1;
                    n0 += 1;
                } else {
                    // increment counts
                    nDA[d][newA] += 1;
                    nDAT[d][newA][t] += 1;
                    nDAST[d][newA][s][t] += 1;
                    nAST[newA][s][t] += 1;
                    nASTW[newA][s][t][w] += 1;
                    nUAT[u][a][t] += 1;
                    nUATS[u][a][t][s] += 1;
                    nEAT[e][a][t] += 1;
                    nEATS[e][a][t][s] += 1;
                }
                //}
                // set new assignments
                levels[n] = l1;
                //aspects[n] = a;
                topics[n] = t;
                sentiments[n] = s;
            }

            if (flag && newA != lastAspect && lastAspect != -1) {
                visitedAspects.add(lastAspect);
            }
            lastAspect = newA;
            nCA[c][newA] +=1;
            sent.setLevels(levels);
            sent.setAspect(newA);
            sent.setTopics(topics);
            sent.setSentiments(sentiments);
        }
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

    public int sampleNewAspectForSentence(int c,int d, Sentence sent, ArrayList<Integer> visitedAspects, int prevAspect) {
        Double sentPTotal = 0.0;
        Double[] sentP = new Double[A];
        Arrays.fill(sentP, 0.0);
        int[] levels = sent.getLevels();
        int[] words = sent.getTokens();
        int[] topics = sent.getTopics();
        int[] sentiments = sent.getSentiments();

        for (int n = 0; n < words.length; n++) {
            Double pTotal = 0.0;
            Double[] p = new Double[A];
            Arrays.fill(p, 0.0);
            for (int i = 0; i < A; i++) {
                if (visitedAspects.contains(i)) {
                    continue;
                }
                if (levels[n] == 0) {
                    p[i] = (nCA[c][i] + alpha3) / (collections.get(c).size() + alpha3Norm);
                } else {
                    p[i] = (nCA[c][i] + alpha3) / (collections.get(c).size() + alpha3Norm)
                            * (nASTW[i][sentiments[n]][topics[n]][words[n]] + alpha6) / (nAST[i][sentiments[n]][topics[n]] + alpha6Norm);
                }
                pTotal += p[i];
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

            sentP[a] += p[a];
        }
        for (int i = 0; i < A; i++) {
            sentPTotal += sentP[i];
        }

        Random r = new Random();
        Double u = r.nextDouble() * sentPTotal;

        int a = 0;
        Double v = 0.0;
        for (int i = 0; i < A; i++) {
            v += sentP[i];

            if (v > u) {
                a = i;
                break;
            }
        }
        double aspectProb = sentP[a];
        //return a;
        if (prevAspect == -1 || aspectProb> aspectThreshold) {
            return a;
        } else {
            return prevAspect;
        }//*/
    }

    public int sampleNewAspect(int d, int w, int l1, int s, int t, ArrayList<Integer> visitedAspects) {
        Double pTotal = 0.0;
        Double[] p = new Double[A];
        Arrays.fill(p, 0.0);
        Double prior, likelihood;
        if (l1 == 0) {
            for (int i = 0; i < A; i++) {
                if (visitedAspects.contains(i)) {
                    continue;
                }
                prior = (nDA[d][i] + alpha3) / (docLength.get(d) + alpha3Norm);
                p[i] = prior;
                pTotal += p[i];
            }
        } else {
            for (int i = 0; i < A; i++) {
                if (visitedAspects.contains(i)) {
                    continue;
                }
                prior = (nDA[d][i] + alpha3) / (docLength.get(d) + alpha3Norm);
                likelihood = (nASTW[i][s][t][w] + alpha6) / (nAST[i][s][t] + alpha6Norm);
                p[i] = prior
                        * likelihood;
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
        if (visitedAspects.contains(a)) {
            System.out.println("stupid logical error");
        }

        return a;
    }

    private int sampleNewTopic(int d, int w, int l1, int a, int s) {

        int maxT = 0;
        for (int t1 = 0; t1 < T; t1++) {
            if (nDAT[d][a][t1] == 0) {
                break;
            }
            maxT = t1;
        }
        Double pTotal = 0.0;
        Double[] p = new Double[maxT + 2];
        Double prior, likelihood;

        if (l1 == 0) {
            if (maxT == 0) {
                prior = (nDAT[d][a][0]) / (nDA[d][a] + topicConcentration);
                p[0] = prior;
                pTotal += p[0];
            } else {
                for (int i = 0; i <= maxT; i++) {
                    prior = (nDAT[d][a][i]) / (nDA[d][a] + topicConcentration);
                    p[i] = prior;
                    pTotal += p[i];
                }
            }
            p[maxT+1] = topicConcentration / (nDA[d][a] + topicConcentration);
        } else {
            if (maxT == 0) {
                prior = (nDAT[d][a][0]) / (nDA[d][a] + topicConcentration);
                p[0] = prior;
                pTotal += p[0];
            } else {
                for (int i = 0; i <= maxT; i++) {
                    p[i] = (nDAT[d][a][i]) / (nDA[d][a] + topicConcentration)
                            * (nASTW[a][s][i][w] + alpha6) / (nAST[a][s][i] + alpha6Norm);
                    pTotal += p[i];
                }
            }
            p[maxT+1] = topicConcentration / (nDA[d][a] + topicConcentration)
                    * 1 / W;
        }
        Random r = new Random();
        Double u = r.nextDouble() * pTotal;

        int t = 0;
        Double v = 0.0;
        for (int i = 0; i <= maxT+1; i++) {
            v += p[i];

            if (v > u) {
                t = i;
                break;
            }
        }
        return t;
    }

    private int sampleNewSentiment(int e, int u, int d, int w, int l1, int a, int t) {
        Double pTotal = 0.0;
        Double[] p = new Double[S];
        double priorEntity, priorAuth, likelihood;
        if (l1 == 0) {
            for (int i = 0; i < S; i++) {
                priorEntity = (nEATS[e][a][t][i] + alpha5[i]) / (nEAT[e][a][t] + alpha5Norm);
                priorAuth = (nUATS[u][a][t][i] + alpha5[i]) / (nUAT[u][a][t] + alpha5Norm);
                p[i] = priorEntity * priorAuth;
                pTotal += p[i];
            }
        } else {
            for (int i = 0; i < S; i++) {
                priorEntity = (nEATS[e][a][t][i] + alpha5[i]) / (nEAT[e][a][t] + alpha5Norm);
                priorAuth = (nUATS[u][a][t][i] + alpha5[i]) / (nUAT[u][a][t] + alpha5Norm);
                likelihood = (nASTW[a][i][t][w] + alpha6) / (nAST[a][i][t] + alpha6Norm);
                p[i] = priorEntity * priorAuth
                        * likelihood;
                pTotal += p[i];
            }
        }
        Random r = new Random();
        Double x = r.nextDouble() * pTotal;

        int s = 0;
        Double v = 0.0;
        for (int i = 0; i < S; i++) {
            v += p[i];
            if (v > x) {
                s = i;
                break;
            }
        }
        return s;
    }

    public void readDocs(HashSet<Entity> inputEntities, Set<String> stopwords, Set<String> collocations) {
        System.out.println("Reading input... Number of entities: " + inputEntities.size());
        wordMap = new HashMap<String, Integer>();
        wordMapInv = new HashMap<Integer, String>();

        collections = new HashMap<>();
        docs = new HashMap<>();
        docLength = new HashMap<>();

        authors = new HashMap<>();
        docAuthors = new HashMap<>();

        entities = new HashMap<>();
        docEntities = new HashMap<>();

        int d = 0;
        int c = 0;
        for (Entity e : inputEntities) {
            String entityName = e.getEntityID() + "";
            ArrayList<Review> reviews = e.getReviews();
            for (Review r : reviews) {
                String author = r.getAuthor();
                ArrayList<Sentence> reviewSentences = new ArrayList<>();
                String[] contentSentences = r.getContent().split("[\\.\\?!]|but|however");
                for (String contentSentence : contentSentences) {
                    String line = Helper.normalizeSentence(contentSentence, stopwords);
                    line = Helper.replaceCollocations(line, collocations);
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
                        Sentence sent = new Sentence(contentSentence, tokens, words);
                        reviewSentences.add(sent);
                        docs.put(sent, d);
                        docLength.put(d, sent.getNwords());
                        if (entities.containsKey(entityName)) {
                            int id = entities.get(entityName);
                            docEntities.put(d, id);
                        } else {
                            int id = entities.size();
                            entities.put(entityName, id);
                            docEntities.put(d, id);
                        }
                        if (authors.containsKey(author)) {
                            int id = authors.get(author);
                            docAuthors.put(d, id);
                        } else {
                            int id = authors.size();
                            authors.put(author, id);
                            docAuthors.put(d, id);
                        }
                        d++;
                    }
                }
                collections.put(c, reviewSentences);
                c++;
                r.setSentences(reviewSentences);
            }

        }
        C = c;
        D = d;
        W = wordMap.size();
        U = authors.size();
        E = entities.size();
        System.out.println(E + " entities");
        System.out.println(U + " authors");
        System.out.println(C + " collections");
        System.out.println(D + " documents");
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
            System.out.println("Error while saving model twords: " + e.getMessage());
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

    public boolean writeTopwordsWithoutSentiment(String filename) {
        Double probThreshold = 0.001;
        try {
            FileWriter fw = new FileWriter(filename);
            BufferedWriter writer = new BufferedWriter(fw);

            if (topWords > W) {
                topWords = W;
            }

            ArrayList<Pair> wordsProbsList = new ArrayList<Pair>();

            for (int a = 0; a < A; a++) {
                writer.write("Aspect " + a + ":\n");
                for (int t = 0; t < T; t++) {
                    writer.write("Topic " + t + ":\n");
                    wordsProbsList = new ArrayList<Pair>();
                    int atCount = 0;
                    for (int d = 0; d < docs.size(); d++) {
                        atCount += nDAT[d][a][t];
                    }
                    for (int w = 0; w < W; w++) {
                        Double prob = getWordProb(a, t, w, atCount);
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
            writer.flush();
            writer.close();
        } catch (Exception e) {
            System.out.println("Error while saving model twords: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Double getWordProb(int a, int t, int w, int atCount) {

        int wordCount = 0;
        for (int s = 0; s < S; s++) {
            wordCount += nASTW[a][s][t][w];
        }
        if (wordCount < wordThreshold) {
            return 0.0;
        }
        Double prob = (Double) (wordCount + alpha6) / (atCount + alpha6Norm);
        return prob;
    }

    public void saveModel(String folderPath) {
        int[] maxT = new int[A];
        for (int a = 0; a < A; a++) {
            for (int d = 0; d < D; d++) {
                for (int t1 = 0; t1 < T; t1++) {
                    if (nDAT[d][a][t1] == 0) {
                        break;
                    }
                    if(maxT[a]<t1)
                        maxT[a] = t1;
                }
            }
        }
        try {
            FileWriter fw1 = new FileWriter(folderPath + File.separator + "wordTopicDist.txt");
            BufferedWriter writer = new BufferedWriter(fw1);
            writer.write(W + "\n");
            Iterator<Integer> it = wordMapInv.keySet().iterator();
            while (it.hasNext()) {
                int w = it.next();
                writer.write(wordMapInv.get(w) + "\t");
                int bgCount = nW[w];
                writer.write(bgCount + "\t");
                for (int a = 0; a < A; a++) {
                    for (int t = 0; t < maxT[a]; t++) {
                        for (int s = 0; s < S; s++) {
                            writer.write(nASTW[a][s][t][w] + "\t");
                        }
                    }
                }
                writer.write("\n");
            }
            writer.flush();
            writer.close();
            fw1 = new FileWriter(folderPath + File.separator + "Params.properties");
            writer = new BufferedWriter(fw1);
            writer.write("Aspect = " + A);
            writer.write("\n");
            writer.write("Topics = ");
            for (int a1 = 0; a1 < A; a1++) {
                writer.write(maxT[a1] + ",");
            }
            writer.write("\n");
            writer.write("Sentiment = " + S);
            writer.flush();
            writer.close();
        } catch (Exception e) {

        }
    }

}
