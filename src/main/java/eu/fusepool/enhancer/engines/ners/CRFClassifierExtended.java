package eu.fusepool.enhancer.engines.ners;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ie.crf.CRFCliqueTree;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Triple;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * CRFClassifierExtended extends the original CRFClassifier to provide functionality
 * for retrieving the confidence scores (or probability values) for the entities.
 * @author Gábor Reményi
 */
public class CRFClassifierExtended<IN extends CoreMap> extends CRFClassifier<IN>{
    /**
     * Returns confidence scores of each token.
     * @param document
     * @return 
     */
    public List<EntityToken> getProbsDocument(List<IN> document) { 
        List<EntityToken> entityTokens = new ArrayList<EntityToken>(); 
        Triple<int[][][], int[], double[][][]> p = documentToDataAndLabels(document);
        CRFCliqueTree<String> cliqueTree = getCliqueTree(p);
        EntityToken token = new EntityToken();
        for (int i = 0; i < cliqueTree.length(); i++) {
            token = new EntityToken();
            IN wi = document.get(i);
            token.label = wi.get(CoreAnnotations.TextAnnotation.class);
            token.begin = wi.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            token.end = wi.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
            for (Iterator<String> iter = classIndex.iterator(); iter.hasNext();) {
                String label = iter.next();
                int index = classIndex.indexOf(label);
                double prob = cliqueTree.prob(i, index);
                token.AddProbability(label, prob);
            }
            entityTokens.add(token);
        }
        return entityTokens;
    }
    
    /**
     * getClassifierNoExceptions method for CRFClassifierExtended class.
     * @param loadPath
     * @return 
     */
    public static CRFClassifierExtended<CoreLabel> getClassifierNoExceptions(String loadPath) {
        CRFClassifierExtended<CoreLabel> crf = new CRFClassifierExtended<CoreLabel>();
        crf.loadClassifierNoExceptions(loadPath);
        return crf;
    }
    
    /**
     * getClassifier method for CRFClassifierExtended class.
     * @param loadPath
     * @return 
     */
    public static CRFClassifierExtended<CoreLabel> getClassifier(String loadPath) throws IOException, ClassCastException, ClassNotFoundException {
        CRFClassifierExtended<CoreLabel> crf = new CRFClassifierExtended<CoreLabel>();
        crf.loadClassifier(loadPath);
        return crf;
    }
}
