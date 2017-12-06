/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Lahari
 * @created on Mar 9, 2015
 */
public class Review {

    private String reviewId;
   // private String authorLocation;
   // private String title;
    private String author;
    private String content;
    private String date;
    
    private HashMap<Integer,Double> rating;
    private Double avgRating;
    private HashMap<String,Set<String>> aspectSentences;
    private HashMap<String,Set<Review>> supportingReviews;
    
    private ArrayList<Sentence> sentences;
    private HashMap<Sentence, HashMap<Integer,Set<Sentence>>> similarReviews;

    
    public Review(String reviewId,String author,HashMap<Integer,Double> rating, String content) {
        this.reviewId = reviewId;
      //  this.authorLocation = authorLocation;
      //  this.title = title;
        this.author = author;
        this.content = content.toLowerCase();
        //this.date = date;
        this.rating = rating;
        //setAvgRating();
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public HashMap<Integer, Double> getRating() {
        return rating;
    }
    
    public HashMap<String, Set<Review>> getSupportingReviews() {
        return supportingReviews;
    }

    public void setSupportingReviews(HashMap<String, Set<Review>> supportingReviews) {
        this.supportingReviews = supportingReviews;
    }

    public HashMap<String, Set<String>> getAspectSentences() {
        return aspectSentences;
    }

    public void setAspectSentences(HashMap<String, Set<String>> aspectSentences) {
        this.aspectSentences = aspectSentences;
    }
    public String getReviewId() {
        return reviewId;
    }

    public String getContent() {
        return content;
    }


    public ArrayList<Sentence> getSentences() {
        return sentences;
    }

    public void setSentences(ArrayList<Sentence> sentences) {
        this.sentences = sentences;
    }

    public HashMap<Sentence, HashMap<Integer,Set<Sentence>>> getSimilarReviews() {
        return similarReviews;
    }

    public void setSimilarReviews(HashMap<Sentence, HashMap<Integer,Set<Sentence>>> similarReviews) {
        this.similarReviews = similarReviews;
    }

    public Double getAvgRating() {
        return avgRating;
    }

    private void setAvgRating() {
        double avg = 0;
        int count =0;
        Iterator<Integer> it = rating.keySet().iterator();
        while(it.hasNext()){
            int a = it.next();
            if(rating.get(a)!=-1){
                avg+=rating.get(a);
                count++;
            }
        }
        if(count>0)
            avgRating = avg/count;
        else
            avgRating = -1.0;
    }
    
    
}
