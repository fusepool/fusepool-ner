package eu.fusepool.enhancer.engines.ners;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.Triple;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalClassifier {
    protected String path;
    protected InputStream modelStream;
    protected AbstractSequenceClassifier<CoreLabel> classifier;
    
    /**
     * Constructs a LocalClassifier object based on the input parameters. This
     * class represents a local classifier object and it uses Stanford based
     * model files to extract entities. The model file needs to be loaded into
     * the memory and used as a CRFClassifier object.
     *
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
        this.classifier = CRFClassifier.getClassifier(model);
    }

    /**
     * Destroys the classifier object.
     */
    public void removeClassifier() {
        this.classifier = null;
    }

    /**
     * Checks whether the classifier was loaded or not.
     *
     * @return
     */
    public boolean isClassifierNull() {
        if (this.classifier == null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * If classifier is loaded then it executes the entity extraction and
     * returns the annotated text.
     *
     * @param text
     * @return
     */
    public String getTaggedText(String text) {
        if (!isClassifierNull()) {
            return this.classifier.classifyWithInlineXML(text);
        } else {
            return null;
        }
    }
    
    /**
     * Based on the classifier, it extracts the entities from the
     * input text, and returns the them in a List.
     *
     * @param text
     * @return
     */
    public List<Entity> GetNamedEntities(String text) throws Exception{
        if (!isClassifierNull()) {
            List<Entity> entities = new ArrayList<Entity>();
            List<Triple<String,Integer,Integer>> tagedTokens = classifier.classifyToCharacterOffsets(text);
            Entity e;

            for(Triple triple : tagedTokens){
                e = new Entity();
                e.begin = ((Integer)triple.second).intValue();
                e.end = ((Integer)triple.third).intValue();
                e.entity = text.substring(e.begin, e.end);
                e.type = triple.first.toString();
                entities.add(e);
                //System.out.println(e.entity + "\t" + e.type + "\t" + e.begin + "\t" + e.end);
            }
            return entities;
        }
        else {
            return null;
        }
    }
}
