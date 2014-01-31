package eu.fusepool.enhancer.engines.ners;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the probabilities for each token of the entity for each type.
 * @author Gábor Reményi
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
     * @param type  Type of the entity token. i.e. PERSON
     * @return 
     */
    public double GetScoreByType(String type){
        return probabilities.get(type);
    }   
    
    /**
     * Adds a new probability score to this instance.
     * @param type  Type of the entity token. i.e. PERSON
     * @param score Score of the entity token for the current type.
     */
    public void AddProbability(String type, Double score){
        probabilities.put(type, score);
    }

    @Override
    public String toString() {
        return "EntityToken{" + "label=" + label + ", begin=" + begin + ", end=" + end + ", probs=" + probabilities.toString() + '}';
    }

}
