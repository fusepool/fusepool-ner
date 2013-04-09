/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.enhancer.engines.ners;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
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
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_LABEL;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_TYPE;


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
   
    @Property
    public static final String MODEL_NAME = "eu.fusepool.enhancer.engines.ners.name";

    @Property
    public static final String MODEL_DESCRIPTION = "eu.fusepool.enhancer.engines.ners.description";
    
    @Property(cardinality = 20)
    public static final String MODEL_TYPES = "eu.fusepool.enhancer.engines.ners.types";
    
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
    private String name;
    private String description;
    private Map<String,UriRef> types;
    private String model;
   
    @Activate
    @Override
    protected void activate(ComponentContext context) throws ConfigurationException {
        super.activate(context);
       
        Dictionary<String,Object> config = context.getProperties();

        Object n = config.get(MODEL_NAME);
        this.name = n == null || n.toString().isEmpty() ? null : n.toString();

        Object d = config.get(MODEL_DESCRIPTION);
        this.description = d == null || d.toString().isEmpty() ? null : d.toString();

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
        Object m = config.get(MODEL_PATH);
        this.model = m == null || m.toString().isEmpty() ? null : m.toString();
        
        if(this.model == null || this.model.isEmpty()){
            throw new ConfigurationException(MODEL_PATH, "Missing or invalid configuration of the supported model");
        }    

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
        return ENHANCE_SYNCHRONOUS; 
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
            entities = ner.GetNamedEntities(text);
            log.info("entities identified: {}",entities);
        }
        catch (Exception e) {
            log.warn("Could not recognize entities", e);
            return;
        }
        
        // add entities to metadata
        if (entities != null) {
            MGraph g = ci.getMetadata();
            ci.getLock().writeLock().lock();
            try {
                for (Entity e : entities) {
                    UriRef textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, this);
                    if(types.get(e.type) != null){
                        g.add(new TripleImpl(textEnhancement, DC_TYPE, types.get(e.type))); 
                    }
                    g.add(new TripleImpl(textEnhancement, ENHANCER_ENTITY_TYPE, new PlainLiteralImpl(e.type))); 
                    g.add(new TripleImpl(textEnhancement, ENHANCER_ENTITY_LABEL, new PlainLiteralImpl(e.entity))); 
                    g.add(new TripleImpl(textEnhancement, ENHANCER_START, new PlainLiteralImpl(Integer.toString(e.begin)))); 
                    g.add(new TripleImpl(textEnhancement, ENHANCER_END, new PlainLiteralImpl(Integer.toString(e.end)))); 
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
