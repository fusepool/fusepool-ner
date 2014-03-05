package eu.fusepool.enhancer.engines.ners;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.Triple;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalClassifier {
    protected String path;
    protected InputStream modelStream;
    protected CRFClassifierExtended<CoreLabel> classifier;
    
    /**
     * Constructs a LocalClassifier object based on the input parameters. This
     * class represents a local classifier object and it uses Stanford based
     * model files to extract entities. The model file needs to be loaded into
     * the memory and used as a CRFClassifier object.
     * @param path
     */
    public LocalClassifier(String model) throws ClassCastException, IOException, ClassNotFoundException{
        super();
        //this.path = "stanbol/datafiles/classifiers/" + model;
        loadClassifier(model);
    }

    /**
     * Loads the classifier (model file) from the given path.
     */
    private void loadClassifier(String model) throws ClassCastException, IOException, ClassNotFoundException{
        classifier = CRFClassifierExtended.getClassifier(model);
    }

    /**
     * Destroys the classifier object.
     */
    public void removeClassifier() {
        classifier = null;
    }

    /**
     * Checks whether the classifier was loaded or not.
     * @return
     */
    public boolean isClassifierNull() {
        if (classifier == null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * If classifier is loaded then it executes the entity extraction and
     * returns the annotated text.
     * @param text
     * @return
     */
    public String getTaggedText(String text) {
        if (!isClassifierNull()) {
            return classifier.classifyWithInlineXML(text);
        } else {
            return null;
        }
    }
    
    /**
     * Based on the classifier, it extracts the entities from the
     * input text, and returns the them in a List.
     * @param text
     * @return
     */
    public List<Entity> GetNamedEntities(String text) throws Exception{
        if (!isClassifierNull()) {
            
            // retrieving the entities
            List<Entity> entities = new ArrayList<Entity>();
            // extract entities from input text
            List<Triple<String,Integer,Integer>> tagedTokens = classifier.classifyToCharacterOffsets(text);
            Entity e;
            for(Triple triple : tagedTokens){
                e = new Entity();
                // get begining of label
                e.begin = ((Integer)triple.second).intValue();
                // get end of label
                e.end = ((Integer)triple.third).intValue();
                // get the label from the text
                e.label = text.substring(e.begin, e.end);
                // get the type
                e.type = triple.first.toString();
                entities.add(e);
            }
            
            // retrieving the confidence scores
            Map<String, EntityToken> entityTokenCollection = new HashMap<String, EntityToken>();
            List<EntityToken> entityTokenList;
            EntityToken previous = null;
            List<List<CoreLabel>> coreLabels = classifier.classify(text);
            for (List<CoreLabel> coreLabel : coreLabels) {
                entityTokenList = classifier.getProbsDocument(coreLabel);
                for (EntityToken entityToken : entityTokenList) {
                    entityToken.previous = previous;
                    entityTokenCollection.put(Integer.toString(entityToken.end), entityToken);
                    previous = entityToken;
                }
            }
            
            // matching the confidence scores with the entities
            for (Entity entity : entities) {
                EntityToken current = entityTokenCollection.get(Integer.toString(entity.end));
                double score = 0;
                int counter = 0;
                if (current != null) {
                    if (current.begin > entity.begin) {
                        score = current.GetScoreByType(entity.type);
                        counter = 1;
                        previous = current.previous;
                        if (previous != null) {
                            while (previous.begin >= entity.begin) {
                                score += previous.GetScoreByType(entity.type);
                                counter++;
                                previous = previous.previous;
                                if (previous == null) {
                                    break;
                                }
                            }
                        }
                        entity.score = (double) score / counter;
                    } else if (current.begin == entity.begin) {
                        entity.score = current.GetScoreByType(entity.type);
                    }
                }
            }
            return entities;
        }
        else {
            return null;
        }
    }
}
