/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package topicModelling;

import java.util.HashMap;

/**
 *
 * @author Lahari
 * @created on Nov 19, 2015
 */
public class Model {
    
    int aspect;
    int topics[];
    int sentiment;
    
    HashMap<String,Integer> wordMap;
    HashMap<Integer,String> InvwordMap;
    
    int[][] wordTopicDist;
    int[] wordsPerTopic;

    public Model(int aspect, int[] topics, int sentiment) {
        this.aspect = aspect;
        this.topics = topics;
        this.sentiment = sentiment;
    }

    public int getAspect() {
        return aspect;
    }

    public void setAspect(int aspect) {
        this.aspect = aspect;
    }

    public int[] getTopics() {
        return topics;
    }

    public void setTopics(int[] topics) {
        this.topics = topics;
    }

    public int getSentiment() {
        return sentiment;
    }

    public void setSentiment(int sentiment) {
        this.sentiment = sentiment;
    }

    public HashMap<String, Integer> getWordMap() {
        return wordMap;
    }

    public void setWordMap(HashMap<String, Integer> wordMap) {
        this.wordMap = wordMap;
    }

    public HashMap<Integer, String> getInvwordMap() {
        return InvwordMap;
    }

    public void setInvwordMap(HashMap<Integer, String> InvwordMap) {
        this.InvwordMap = InvwordMap;
    }

    public int[][] getWordTopicDist() {
        return wordTopicDist;
    }

    public void setWordTopicDist(int[][] wordTopicDist) {
        this.wordTopicDist = wordTopicDist;
    }

    public int[] getWordsPerTopic() {
        return wordsPerTopic;
    }

    public void setWordsPerTopic(int[] wordsPerTopic) {
        this.wordsPerTopic = wordsPerTopic;
    }
    
    

}
