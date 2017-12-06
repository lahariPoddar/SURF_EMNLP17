/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package elements;

import java.util.ArrayList;

/**
 *
 * @author Lahari
 * @created on Aug 4, 2015
 */
public class Restaurant extends Entity{

    public Restaurant(String restaurantName, int restaurantID, ArrayList<Review> reviews) {
        this.entityID = restaurantID;
        this.entityName = restaurantName;
        this.reviews = reviews;
    }

}
