/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package elements;

/**
 *
 * @author Lahari
 * @created on Jul 9, 2015
 */
public class Sentence 
{    
	private String sent; // the original sentence
        private String sentId;
	private int nwords; // the number of tokens in the sentence
	
	
	private int aspect;
	private int[] tokens; // the tokens in this sentence
        private String[] words;
   
	private int[] topics; // the topics that generated these tokens
	private int[] sentiments;
        private int[] aspects;
        private int[] levels;
        
        private Double[] probabilities;
        private Double[] depth;
	
	/**
	 * @param sent The original text of the sentence
	 */
	public Sentence(String sent,  int[] tokens, String[] words)
	{
                
		this.sent = sent;
		this.words = words;
		this.tokens = tokens;
		nwords = tokens.length;
		aspects = new int[tokens.length];
                topics = new int[tokens.length];
                sentiments = new int[tokens.length];
                levels = new int[tokens.length];
                probabilities = new Double[tokens.length];
	}

    public String getSentId() {
        return sentId;
    }

    public void setSentId(String sentId) {
        this.sentId = sentId;
    }
        
        
      
    public String getSent() {
        return sent;
    }

    public void setSent(String sent) {
        this.sent = sent;
    }

    public int getNwords() {
        return nwords;
    }

    public void setNwords(int nwords) {
        this.nwords = nwords;
    }

    public int getAspect() {
        return aspect;
    }

    public void setAspect(int aspect) {
        this.aspect = aspect;
    }

    public int[] getTokens() {
        return tokens;
    }

    public void setTokens(int[] tokens) {
        this.tokens = tokens;
    }

    public String[] getWords() {
        return words;
    }

    public void setWords(String[] words) {
        this.words = words;
    }

    public int[] getTopics() {
        return topics;
    }

    public void setTopics(int[] topics) {
        this.topics = topics;
    }

    public int[] getSentiments() {
        return sentiments;
    }

    public void setSentiments(int[] sentiments) {
        this.sentiments = sentiments;
    }

    public int[] getAspects() {
        return aspects;
    }

    public void setAspects(int[] aspects) {
        this.aspects = aspects;
    }

    public int[] getLevels() {
        return levels;
    }

    public void setLevels(int[] levels) {
        this.levels = levels;
    }

    public Double[] getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(Double[] probabilities) {
        this.probabilities = probabilities;
    }

    public Double[] getDepth() {
        return depth;
    }

    public void setDepth(Double[] depth) {
        this.depth = depth;
    }

    
 
 
}


