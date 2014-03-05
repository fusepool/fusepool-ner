package eu.fusepool.enhancer.engines.ners;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;


@Component(configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE,
    metatype = true, immediate = true, inherit = true)
@Service
@Properties(value = {
    @Property(name = EnhancementEngine.PROPERTY_NAME)
})
public class NerEnhancementEngine  
        extends AbstractEnhancementEngine<RuntimeException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    /**
     * A short description about the NER model.
     */
    @Property
    public static final String MODEL_DESCRIPTION = "eu.fusepool.enhancer.engines.ners.description";
    /**
     * The type and the URI of the type of the this model separated by a semicolon. The type is defined at the building of 
     * the model file. (e.g. PERSON, LOCATION) The syntax is "{type};{uri}", for example "PERSON;http://dbpedia.org/ontology/Person"
     */
    @Property(cardinality = 20)
    public static final String MODEL_TYPES = "eu.fusepool.enhancer.engines.ners.types";
    /**
     * Relative path of the model file. (The name of the model file e.g. "en-ner-disease.crf.ser.gz")
     */
    @Property
    public static final String MODEL_PATH = "eu.fusepool.enhancer.engines.ners.model";
    
    /**
     * Default value for the {@link Constants#SERVICE_RANKING} used by this engine.
     * This is a negative value to allow easy replacement by this engine depending
     * to a remote service with one that does not have this requirement
     */
    public static final int DEFAULT_SERVICE_RANKING = 100;
    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link EnhancementJobManager#DEFAULT_ORDER}
     */
    public static final Integer defaultOrder = ORDERING_EXTRACTION_ENHANCEMENT;
    /**
     * This contains the only MIME type directly supported by this enhancement
     * engine.
     */
    private static final String TEXT_PLAIN_MIMETYPE = "text/plain";
    /**
     * Set containing the only supported mime type {@link #TEXT_PLAIN_MIMETYPE}
     */
    private static final Set<String> SUPPORTED_MIMTYPES = Collections.singleton(TEXT_PLAIN_MIMETYPE);
    /**
     * This contains the logger.
     */
    private static final Logger log = LoggerFactory.getLogger(NerEnhancementEngine.class);
    
    private NER ner;
    private String description;
    private Map<String,UriRef> types;
    private String model;
   
    @Activate
    @Override
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);
       // read configuration
        Dictionary<String,Object> config = context.getProperties();
        // reading model description property
        Object d = config.get(MODEL_DESCRIPTION);
        this.description = d == null || d.toString().isEmpty() ? null : d.toString();
        // reading model types property
        Object t = config.get(MODEL_TYPES);
        if(t instanceof Iterable<?>){
            types = new HashMap<String,UriRef>();
            for(Object o : (Iterable<Object>)t){
                if(o != null && !o.toString().isEmpty()){
                    String[] val = o.toString().split(";", 2);
                    
                    types.put(val[0], new UriRef(val[1]));
                } else {
                    log.warn("Model types configuration '{}' contained illegal value '{}' -> removed",t,o);
                }
            }
        } else if(t.getClass().isArray()){
            types = new HashMap<String,UriRef>();
            for(Object modelObj : (Object[])t){
                if(modelObj != null){
                    String[] val = modelObj.toString().split(";", 2);
                    
                    types.put(val[0], new UriRef(val[1]));
                } else {
                    log.warn("Model types configuration '{}' contained illegal value '{}' -> removed",
                    Arrays.toString((Object[])t),t);
                }
            }
        } else {
            types = null;
        }
        // reading model path property
        Object m = config.get(MODEL_PATH);
        this.model = m == null || m.toString().isEmpty() ? null : m.toString();
        // model path cannot be null or empty
        if(this.model == null || this.model.isEmpty()){
            throw new ConfigurationException(MODEL_PATH, "Missing or invalid configuration of the supported model");
        }    
        // get absolute path of the model file
        URL url = this.getClass().getResource("/classifiers/" + model);
        
        if(url == null)
        {
            log.warn("Invalid path for classifier file: " + model);
        }
        else
        {
            try {
                ner = new NER(url.getFile());
            } catch (ClassCastException ex) {
                log.warn("Unable to load classifier file for NerEnhancementEngine (file: "+model+")", ex);
            } catch (IOException ex) {
                log.warn("Unable to load classifier file for NerEnhancementEngine (file: "+model+")", ex);
            } catch (ClassNotFoundException ex) {
                log.warn("Unable to load classifier file for NerEnhancementEngine (file: "+model+")", ex);
            }
        }
    }

    @Deactivate
    @Override
    protected void deactivate(ComponentContext context) {
        super.deactivate(context);
        ner = null;
    }            

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        try {
            if ((ci.getBlob() == null)
                    || (ci.getBlob().getStream().read() == -1)) {
                return CANNOT_ENHANCE;
            }
        } catch (IOException e) {
            log.error("Failed to get the text for "
                    + "enhancement of content: " + ci.getUri(), e);
            throw new InvalidContentException(this, ci, e);
        }
        // no reason why we should require to be executed synchronously
        return ENHANCE_ASYNC;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        Map.Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, SUPPORTED_MIMTYPES);
        if(contentPart == null){
            throw new IllegalStateException("No ContentPart with Mimetype '"
                    + TEXT_PLAIN_MIMETYPE+"' found for ContentItem "+ci.getUri()
                    + ": This is also checked in the canEnhance method! -> This "
                    + "indicated an Bug in the implementation of the "
                    + "EnhancementJobManager!");
        }
        String text = "";
        try {
            // get text from the input
            text = ContentItemHelper.getText(contentPart.getValue());
        } catch (IOException e) {
            throw new InvalidContentException(this, ci, e);
        }
        
        if (text.trim().length() == 0) {
            log.info("No text contained in ContentPart {} of ContentItem {}",
                    contentPart.getKey(), ci.getUri());
            return;
        }
        
        List<Entity> entities = null;
        try {
            // extract entities from input text
            entities = ner.GetNamedEntities(text);
            log.info("entities identified: {}",entities);
        }
        catch (Exception e) {
            log.warn("Could not recognize entities", e);
            return;
        }
        
        // add entities to metadata
        if (entities != null) {
            LiteralFactory literalFactory = LiteralFactory.getInstance();
            MGraph g = ci.getMetadata();
            ci.getLock().writeLock().lock();
            try {
                for (Entity e : entities) {
                    // generate a new URI for each entity
                    String uri = "urn:fusepool:" + UUID.randomUUID().toString();
                    UriRef entity_uri = new UriRef(uri);
                    UriRef textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
                    if(types.get(e.type) != null){
                        g.add(new TripleImpl(textEnhancement, DC_TYPE, types.get(e.type))); 
                    }
                    g.add(new TripleImpl(textEnhancement, ENHANCER_ENTITY_TYPE, new PlainLiteralImpl(e.type))); 
                    g.add(new TripleImpl(textEnhancement, ENHANCER_ENTITY_LABEL, new PlainLiteralImpl(e.label))); 
                    g.add(new TripleImpl(textEnhancement, ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(e.score)));
                    g.add(new TripleImpl(textEnhancement, ENHANCER_START, new PlainLiteralImpl(Integer.toString(e.begin)))); 
                    g.add(new TripleImpl(textEnhancement, ENHANCER_END, new PlainLiteralImpl(Integer.toString(e.end)))); 
                    g.add(new TripleImpl(textEnhancement, ENHANCER_ENTITY_REFERENCE, entity_uri)); 
                    g.add(new TripleImpl(entity_uri, org.apache.clerezza.rdf.ontologies.RDF.type, types.get(e.type)));
                    g.add(new TripleImpl(entity_uri, org.apache.clerezza.rdf.ontologies.RDFS.label, new PlainLiteralImpl(e.label)));                    
                }
            } finally {
                ci.getLock().writeLock().unlock();
            }
        }
    }

    @Override
    public Map<String, Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING, (Object) defaultOrder));
    }
}
