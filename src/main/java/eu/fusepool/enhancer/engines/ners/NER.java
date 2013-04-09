/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.ners;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * This class is responsible for the entity extraction and is based on local 
 * classifiers. Local classifiers are using Stanford based model files to 
 * extract entities from plain texts.
 * 
 * @author Gabor
 */
public class NER {
    LocalClassifier localClassifier;
    
    /**
     * Constructs a NER object, using multiple model files.
     */
    public NER(String model) throws ClassCastException, IOException, ClassNotFoundException{
        super();

        if (localClassifier != null) {
            RemoveClassifier();
        }
        
        localClassifier = new LocalClassifier(model);
    }

    /**
     * Removes classifier if it is no longer needed. In case of local
     * classifier the memory has to be freed by explicitly calling the garbage
     * collection.
     */
    private void RemoveClassifier() {
        localClassifier.removeClassifier();
        localClassifier = null;
        System.gc();
    }

    /**
     * Based on the classifier, it extracts the entities from the
     * input text, and returns the them in a List.
     *
     * @param text
     * @return
     */
    public List<Entity> GetNamedEntities(String text) throws Exception{
        return localClassifier.GetNamedEntities(text);
    }
}
