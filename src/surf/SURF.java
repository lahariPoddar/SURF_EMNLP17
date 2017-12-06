/*
 * Author: Lahari
 * Date: Nov 2016
 */
package surf;

import java.io.File;
import java.util.*;
import elements.*;
import Util.IOUtil;
import topicModelling.*;

public class SURF {
    
    public static void modelranking(LinkedHashMap<String, LinkedHashMap<String, String[]>> similarSentences, String model, int iter) throws InterruptedException {
        IOUtil util = new IOUtil();
        util.readJSONFiles();
        //util.readTxtFiles('y');
        int[] dimensions = runTopicModel(util, model,iter);
        
        Model m = util.readModel("output" + File.separator + model + File.separator + "model_TA"+File.separator);
        int[][] testDocs = util.readTestJSONDocs("data/TripAdvisorJson/sample");
        double likelihood = evaluateTopicModel(m,testDocs);
        System.out.println("perplexity = "+likelihood);//*/
        
    }

    private static int[] runTopicModel(IOUtil util, String model,int iters) {
        int[] dimensions = new int[3];
        System.out.println("modelName: " + model);
        if (model.equals("ATS")) {
            Double alpha10 = 2.0 * 1;
            Double alpha11 = 2.0 * 1;
            Double alpha20 = 80.0 * 1;
            Double alpha21 = 0.1 * 1;
            Double alpha3 = 0.1 * 1;
            Double alpha4 = 0.1 * 1;

            Double[] alpha5 = {1.0, 1.0, 3.0};
            Double alpha6 = 0.01 * 1;
            Double labelPrior = 0.0;

            int aspect = util.A;
            int topic = 2;

            ATS topicModelling = new ATS(topic, aspect, alpha10, alpha11, alpha20, alpha21, alpha3, alpha4, alpha5, alpha6, labelPrior);
            topicModelling.readDocs(util.getEntities(), util.stopwords, util.collocations);
            topicModelling.initialize(util.aspectWords);

            System.out.println("Sampling...");

            for (int iter = 1; iter <= iters; iter++) {
                System.out.println("Iteration " + iter);
                topicModelling.doSampling();
            }

            // write variable assignments
            topicModelling.setProbabilities(util.getEntities());
            topicModelling.writeTopwords("output" + File.separator + "ATS" + File.separator + "topwords.txt");
            topicModelling.saveModel("output" + File.separator + "ATS" + File.separator + "model_TA");
            dimensions[0] = topicModelling.A;
            dimensions[1] = topicModelling.T;
            dimensions[2] = topicModelling.S;
        } else if (model.equals("AuthorATS")) {
            Double alpha10 = 2.0 * 1;
            Double alpha11 = 2.0 * 1;
            Double alpha20 = 100.0 * 1;
            Double alpha21 = 0.1 * 1;
            Double alpha3 = 0.1 * 1;
            Double alpha4 = 0.1 * 1;

            Double aspectPrior = 0.01;//{.1,.5,.5,.3,.1,.1};
            Double[] alpha5 = {1.0, 1.0, 1.0};
            Double alpha6 = 0.1 * 1;
            Double labelPrior = 0.0;

            int aspect =  util.A;
            int topic = 4;
            //int[] topics = {1,10,7,7,3,3};
            AuthorATS topicModelling = new AuthorATS(topic, aspect, alpha10, alpha11, alpha20, alpha21, aspectPrior, alpha4, alpha5, alpha6, labelPrior);
            topicModelling.readDocs(util.getEntities(), util.stopwords, util.collocations);
            topicModelling.initialize(util.aspectWords);

            System.out.println("Sampling...");

            boolean flag = true;
            for (int iter = 1; iter <= iters; iter++) {
                System.out.println("Iteration " + iter);
                topicModelling.doSampling(flag);
            }
            // write variable assignments
            topicModelling.setProbabilities(util.getEntities());
            topicModelling.writeTopwords("output" + File.separator + "AuthorATS" + File.separator + "topwords.txt");
            topicModelling.saveModel("output" + File.separator + "AuthorATS" + File.separator + "model_TA");
            dimensions[0] = topicModelling.A;
            dimensions[1] = topicModelling.T;
            dimensions[2] = topicModelling.S;
        } else if (model.equals("ATSNonParam")) {
            Double alpha10 = 2.0 * 1;
            Double alpha11 = 2.0 * 1;
            Double alpha20 = 80.0 * 1;
            Double alpha21 = 0.1 * 1;
            Double alpha3 = 0.1 * 1;
            Double alpha4 = 0.0000001;

            Double[] alpha5 = {1.0, 1.0, 3.0};
            Double alpha6 = 0.01 * 1;
            Double labelPrior = 0.0;

            int aspect = util.A;
            int topic = 11; //maximum number of topics

            ATSNonParam topicModelling = new ATSNonParam(topic, aspect, alpha10, alpha11, alpha20, alpha21, alpha3, alpha4, alpha5, alpha6, labelPrior);
            topicModelling.readDocs(util.getEntities(), util.stopwords, util.collocations);
            topicModelling.initialize(util.aspectWords);

            System.out.println("Sampling...");

            boolean flag = false;
            for (int iter = 1; iter <= iters; iter++) {
                System.out.println("Iteration " + iter);
                if (iter >= iters / 2) {
                    flag = true;
                }
                topicModelling.doSampling(flag);
            }
            // write variable assignments
            topicModelling.setProbabilities(util.getEntities());
            topicModelling.writeTopwords("output" + File.separator + "ATSNonParam" + File.separator + "topwords.txt");
            topicModelling.saveModel("output" + File.separator + "ATSNonParam" + File.separator + "model_TA");
            dimensions[0] = topicModelling.A;
            dimensions[1] = topicModelling.T;
            dimensions[2] = topicModelling.S;
        } 
        return dimensions;
    }

    
    private static double evaluateTopicModel(Model m, int[][] testing) {
        double perplexity = 0.0;
        Double beta = 0.001 * 1;
        int aspects = m.getAspect();
        int sentiment = m.getSentiment();
        int numTopics = 1; // background
        for(int a=0; a<aspects; a++){
            numTopics += m.getTopics()[a]*sentiment;
        }
        double[] alpha = new double[numTopics];
        double alphaSum =0.0;
        
        alpha[0] = 1.0; // background topic
        alphaSum+=alpha[0];
        
        for(int a=0; a<aspects; a++){
            for(int t=0; t<m.getTopics()[a]; t++){
                for(int s=0; s< sentiment; s++){
                    int index = t*sentiment+s;//+ 1; // offset for current aspect and 1 for background aspect
                    for(int a1=0; a1<a; a1++){
                        index+= m.getTopics()[a1]*sentiment;
                    }
                    alpha[index] = 0.01;//aspectPrior[a]*topicPrior*sentimentPrior[s];
                    alphaSum+=alpha[index];
                }
            }
        }
        
        PerplexityEvaluation eval = new PerplexityEvaluation(numTopics, alpha, alphaSum, beta, m.getWordTopicDist(), m.getWordsPerTopic());
        
        perplexity = eval.evaluateLeftToRight(testing, 50, true);
        System.out.println("perplexity : "+ perplexity);
        return perplexity;
    }
    
    public static void main(String args[]) throws InterruptedException {
        String modelName = "ATSNonParam";
        SURF.modelranking(null, modelName, 20);
        System.out.println("Finished building model for: " + modelName);
    }
    
}
