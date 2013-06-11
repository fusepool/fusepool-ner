/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.ners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  
 * @author Gabor
 */
public class EntityToken {
    public String label;
    public int begin;
    public int end;
    public EntityToken previous;
    Map<String, Double> probabilities;

    public EntityToken() {
        probabilities = new HashMap<String, Double>();
    }
    
    /**
     * Returns the probability score which belongs to the given type.
     * @param type
     * @return 
     */
    public double GetScoreByType(String type){
        return probabilities.get(type);
    }   
    
    /**
     * Adds a new probability score to this instance.
     * @param type
     * @param score 
     */
    public void AddProbability(String type, Double score){
        probabilities.put(type, score);
    }

    @Override
    public String toString() {
        return "EntityToken{" + "label=" + label + ", begin=" + begin + ", end=" + end + ", probs=" + probabilities.toString() + '}';
    }

}
