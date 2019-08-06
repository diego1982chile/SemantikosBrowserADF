package cl.minsal.semantikos.browser;

import cl.minsal.semantikos.clients.ServiceLocator;
import cl.minsal.semantikos.kernel.components.*;
import cl.minsal.semantikos.kernel.componentsweb.TimeOutWeb;
import cl.minsal.semantikos.model.ConceptSMTK;
import cl.minsal.semantikos.model.categories.Category;
import cl.minsal.semantikos.model.descriptions.Description;
import cl.minsal.semantikos.model.descriptions.DescriptionTypeFactory;
import cl.minsal.semantikos.model.queries.BrowserQuery;
import cl.minsal.semantikos.model.relationships.Target;
import cl.minsal.semantikos.model.snomedct.ConceptSCT;
import cl.minsal.semantikos.model.tags.Tag;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.naming.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import cl.minsal.semantikos.client.EJBLocator;

import static java.lang.System.currentTimeMillis;

import java.util.logging.Logger;

import oracle.adf.view.rich.context.AdfFacesContext;
import oracle.adf.view.rich.render.ClientEvent;

//import oracle.as.management.translation.Category;

@ManagedBean
@SessionScoped
public class BrowserBean implements Serializable {

    private static final long serialVersionUID = 20120925L;

    /**
     * Variables para el browser
     */
    //static final Logger logger = LoggerFactory.getLogger(BrowserBean.class);

    /**
     * Objeto de consulta: contiene todos los filtros y columnas necesarios para el despliegue de los resultados en el navegador
     */
    private BrowserQuery browserQuery;

    /**
     * Lista de categorías para el despliegue del filtro por categorías
     */
    private List<Category> categories = new ArrayList<Category>();

    /**
     * Lista de tags para el despliegue del filtro por tags
     */
    private List<Tag> tags = new ArrayList<Tag>();
    
    /**
     * Lista de tags para el despliegue del filtro por tags
     */
    private List<ConceptSMTK> concepts = new ArrayList<ConceptSMTK>();

    /**
     * Lista de conceptos para el despliegue del resultado de la consulta
     */
    //private LazyDataModel<ConceptSMTK> concepts;
    private ConceptSMTK conceptSelected;

    private Description descriptionSelected;

    /**
     * Indica si cambió algún filtro. Se utiliza para resetear la páginación al comienzo si se ha filtrado
     */
    private boolean isFilterChanged;
    
    //@EJB
    private QueryManager queryManager;

    //@EJB
    private TagManager tagManager;

    //@EJB
    private CategoryManager categoryManager;

    //@EJB
    private ConceptManager conceptManager;

    //@EJB
    private DescriptionManager descriptionManager;

    //@EJB
    private RelationshipManager relationshipManager;
    
    //@EJB
    private TimeOutWeb timeOutWeb;

    private boolean snomedCT;
    
    private int rowCount;


    @PostConstruct
    protected void initialize() {  

        queryManager = (QueryManager) EJBLocator.getInstance().getService(QueryManager.class);
        
        tagManager = (TagManager) EJBLocator.getInstance().getService(TagManager.class);;
        
        categoryManager = (CategoryManager) EJBLocator.getInstance().getService(CategoryManager.class);
        
        conceptManager = (ConceptManager) EJBLocator.getInstance().getService(ConceptManager.class);
        
        descriptionManager = (DescriptionManager) EJBLocator.getInstance().getService(DescriptionManager.class);

        relationshipManager = (RelationshipManager) EJBLocator.getInstance().getService(RelationshipManager.class);            
                
        timeOutWeb = (TimeOutWeb) EJBLocator.getInstance().getService(TimeOutWeb.class);    
        tagManager = (TagManager) EJBLocator.getInstance().getService(TagManager.class);
        categoryManager = (CategoryManager) EJBLocator.getInstance().getService(CategoryManager.class);

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        int timeOut = timeOutWeb.getTimeOut();
        request.getSession().setMaxInactiveInterval(timeOut);

        //ServiceLocator.getInstance().closeContext();
        tags = tagManager.getAllTags();
        categories = categoryManager.getCategories();   
        browserQuery = queryManager.getDefaultBrowserQuery();
        browserQuery.setQuery("");

    }
    

    private String getServiceName(Type type) {
        String[] tokens = type.toString().split(" ")[1].split("\\.");
        return tokens[tokens.length-1]+"Impl";
    }

    private String getViewClassName(Type type) {
        return type.toString().split(" ")[1];
    }

    /**
     * Este método es el responsable de ejecutar la consulta
     */
    public void executeQuery() {        

        /**
         * Si el objeto de consulta no está inicializado, inicializarlo
         */
        if(browserQuery == null) {
            browserQuery = queryManager.getDefaultBrowserQuery();
        }

        browserQuery.setTruncateMatch(false);

        /**
         * Si la consulta viene nula o vacía retornan inmediatamente
         */
        if(browserQuery.getQuery() == null || browserQuery.getQuery().isEmpty()) {
            return;
        }

        /**
         * Ejecutar la consulta
         */                        
        concepts = queryManager.executeQuery(browserQuery);
        
        rowCount = queryManager.countQueryResults(browserQuery);

        browserQuery.setTruncateMatch(true);

        for (ConceptSMTK conceptSMTK : queryManager.executeQuery(browserQuery)) {
            if(!concepts.contains(conceptSMTK)) {
                concepts.add(conceptSMTK);
            }
        }
        
        System.out.println("conceptSMTKs.size() = " + concepts.size());

        if(rowCount < concepts.size()) {
            rowCount = queryManager.countQueryResults(browserQuery);
            //rowCount = rowCount + queryManager.countQueryResults(browserQuery);
        }  
        

    }

    public List<Description> searchSuggestedDescriptions(String term) {
        isFilterChanged = true;
        browserQuery.setQuery(term);
        List<Description> suggestedDescriptions = new ArrayList<>();
        DescriptionTypeFactory.DUMMY_DESCRIPTION.setTerm("");
        suggestedDescriptions.add(DescriptionTypeFactory.DUMMY_DESCRIPTION);
        suggestedDescriptions.addAll(descriptionManager.searchDescriptionsSuggested(term, categories, null, null));
        for (Description suggestedDescription : suggestedDescriptions) {
            if(suggestedDescription.getTerm().length() > 90) {
                suggestedDescription.setTerm(suggestedDescription.getTerm().substring(0, 91).concat("..."));
            }
        }
        return suggestedDescriptions;
    }

    public void test() throws IOException {

        /*
        if(descriptionSelected == null) {
            descriptionSelected = DescriptionTypeFactory.DUMMY_DESCRIPTION;
        }
        */

        /**
         * Si no se ha seleccionado ninguna descripción sugerida,
         */
        if(descriptionSelected.equals(DescriptionTypeFactory.DUMMY_DESCRIPTION)) {
            descriptionSelected.setTerm(browserQuery.getQuery());
        }
        else {
            //browserQuery.setQuery(descriptionSelected.getTerm());
            browserQuery.setQuery(descriptionSelected.getConceptSMTK().getConceptID());
        }
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        if(request.getRequestURI().equals("/views/home.xhtml")) {
            ExternalContext eContext = FacesContext.getCurrentInstance().getExternalContext();
            eContext.redirect(eContext.getRequestContextPath() + "/views/concepts");
        }
    }

    public void redirect() throws IOException {
        ExternalContext eContext = FacesContext.getCurrentInstance().getExternalContext();
        if(browserQuery.getQuery() != null && browserQuery.getQuery().length() >= 3) {            
            eContext.redirect(eContext.getRequestContextPath() + "/views/concepts");
        }
    }

    public void redirectSnomedCT() throws IOException {

        ExternalContext eContext = FacesContext.getCurrentInstance().getExternalContext();

        if(snomedCT) {
            eContext.redirect(eContext.getRequestContextPath() + "/views/snomed/concepts");
        }
        else {
            eContext.redirect(eContext.getRequestContextPath() + "/views/concepts");
        }
    }

    public void invalidate() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        request.getSession().invalidate();
        context.getExternalContext().redirect(context.getExternalContext().getRequestContextPath());
    }

    public ConceptSMTK getConceptSelected() {
        return conceptSelected;
    }

    public void setConceptSelected(ConceptSMTK conceptSelected) {
        this.conceptSelected = conceptSelected;
    }
    
    public List<ConceptSMTK> getConcepts() {
        return concepts;
    }

    public void setConcepts(List<ConceptSMTK> concepts) {
        this.concepts = concepts;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public CategoryManager getCategoryManager() {
        return categoryManager;
    }

    public void setCategoryManager(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    public ConceptManager getConceptManager() {
        return conceptManager;
    }

    public void setConceptManager(ConceptManager conceptManager) {
        this.conceptManager = conceptManager;
    }

    public BrowserQuery getBrowserQuery() {
        return browserQuery;
    }

    public void setBrowserQuery(BrowserQuery browserQuery) {
        this.browserQuery = browserQuery;
    }

    public QueryManager getQueryManager() {
        return queryManager;
    }

    public void setQueryManager(QueryManager queryManager) {
        this.queryManager = queryManager;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public void setTagManager(TagManager tagManager) {
        this.tagManager = tagManager;
    }

    public boolean isFilterChanged() {
        return isFilterChanged;
    }

    public void setFilterChanged(boolean filterChanged) {
        isFilterChanged = filterChanged;
    }

    public DescriptionManager getDescriptionManager() {
        return descriptionManager;
    }

    public void setDescriptionManager(DescriptionManager descriptionManager) {
        this.descriptionManager = descriptionManager;
    }

    public RelationshipManager getRelationshipManager() {
        return relationshipManager;
    }

    public void setRelationshipManager(RelationshipManager relationshipManager) {
        this.relationshipManager = relationshipManager;
    }

    public Description getDescriptionSelected() {
        return descriptionSelected;
    }

    public void setDescriptionSelected(Description descriptionSelected) {
        this.descriptionSelected = descriptionSelected;
    }

    public boolean isSnomedCT() {
        return snomedCT;
    }

    public void setSnomedCT(boolean snomedCT) {
        this.snomedCT = snomedCT;
    }

}
