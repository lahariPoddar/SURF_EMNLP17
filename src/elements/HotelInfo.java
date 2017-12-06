/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package elements;

import Util.MathUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author Lahari
 * @created on Mar 9, 2015
 */


public class HotelInfo extends Entity {
 
    private double[] sdAspectRatings;
    private int A;
    
    public HotelInfo(String hotelName, String price, String address, int hotelID,int A, ArrayList<Review> reviews) {
        this.entityName = hotelName;
     //   this.price = price;
     //   this.address = address;
        this.entityID = hotelID;
        this.reviews = reviews;
        this.A = A;
        //computeRatings();
    }

    
    public int getHotelID() {
        return entityID;
    }

    public ArrayList<Review> getReviews() {
        return reviews;
    }
    
    public void setReviews(ArrayList<Review> reviews) {
        this.reviews = reviews;
    }

    public String getHotelName() {
        return entityName;
    }

    public void setHotelName(String hotelName) {
        this.entityName = hotelName;
    }

    private void computeRatings() {
        sdAspectRatings = new double[A];
        int noOfReviews = reviews.size();
        HashMap<Integer,ArrayList<Double>> aspectRatings = new HashMap<>();;
        for(int i=0;i<noOfReviews;i++){
            Review r = reviews.get(i);
            HashMap<Integer,Double> ratngs = r.getRating();
            for(int a=0;a<A; a++){
                if(ratngs.get(a)!= -1){
                    if(aspectRatings.containsKey(a)){
                        ArrayList<Double> aspectRating = aspectRatings.get(a);
                        aspectRating.add(ratngs.get(a));
                    }else{
                        ArrayList<Double> aspectRating = new ArrayList<>();
                        aspectRating.add(ratngs.get(a));
                        aspectRatings.put(a, aspectRating);
                    }
                }
            }
        }
        
        for(int a=0;a<A;a++){
            Double[] ratings = new Double[aspectRatings.get(a).size()];
            ratings = aspectRatings.get(a).toArray(ratings);
            sdAspectRatings[a] = MathUtil.findDeviation(ratings);
        }
    }
    

}
