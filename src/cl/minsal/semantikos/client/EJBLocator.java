package cl.minsal.semantikos.client; /**
 * Created by root on 15-05-17.
 */

import javax.naming.*;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class EJBLocator {

    private static final EJBLocator instance = new EJBLocator();
    private static Context context;
    private static Properties props;

    /** Mapa de interfaces por su nombre. */
    private Map<String, Object> servicesByName;

    private static String APP_NAME = "SemantikosCentral/";
    private static String MODULE_NAME = "SemantikosKernelEJB/";

    private void lookupRemoteStatelessEJB(Type type) throws NamingException {

        //final String version =  getClass().getPackage().getImplementationVersion();
        // The app name is the application name of the deployed EJBs. This is typically the ear name
        // without the .ear suffix. However, the application name could be overridden in the application.xml of the
        // EJB deployment on the server.
        // Since we haven't deployed the application as a .ear, the app name for us will be an empty string
        final String appName = APP_NAME;
        // This is the module name of the deployed EJBs on the server. This is typically the jar name of the
        // EJB deployment, without the .jar suffix, but can be overridden via the ejb-jar.xml
        // In this example, we have deployed the EJBs in a jboss-as-ejb-remote-app.jar, so the module name is
        // jboss-as-ejb-remote-app
        final String moduleName = MODULE_NAME;
        // AS7 allows each deployment to have an (optional) distinct name. We haven't specified a distinct name for
        // our EJB deployment, so this is an empty string
        final String distinctName = "";
        // The EJB name which by default is the simple class name of the bean implementation class
        final String beanName = getServiceName(type);
        // the remote view fully qualified class name
        final String viewClassName = getViewClassName(type);
        // let's do the lookup (notice the ?stateful string as the last part of the jndi name for stateful bean lookup)
        //String jndiname = "ejb:" + appName + moduleName + beanName + "!" + viewClassName;
        String jndiname = "ejb:" + appName + moduleName + beanName + "!" + viewClassName;
        //String jndiname = appName + moduleName + beanName + "!" + viewClassName;

        Object remoteEjb = context.lookup(jndiname);

        this.servicesByName.put(getServiceName(type), remoteEjb);
    }

    /**
     * Constructor privado para el Singleton del Factory.
     * EJBClientContext using EJBClientAPI
     */
    private EJBLocator() {

        props = new Properties();
        //props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        //props.put(InitialContext.SECURITY_PRINCIPAL, "user@admin.cl");
        //props.put(InitialContext.SECURITY_CREDENTIALS, "1234567z");
        //props.put("remote.connection.default.username", "user@admin.cl");
        //props.put("remote.connection.default.password", "1234567z");
        //props.put("jboss.naming.client.ejb.context", "true");
        //props.setProperty("org.jboss.ejb.client.scoped.context", "true");

        try {
            context = new InitialContext(props);
            //context = new InitialContext(properties);
            //Autenticar usuario guest para la posterior invocacion de componentes durante despliegue
            //AuthenticationManager authManager = (AuthenticationManager) getService(AuthenticationManager.class);
            //authManager.
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        this.servicesByName = new ConcurrentHashMap<>();
    }

    public static EJBLocator getInstance() {
        return instance;
    }

    /**
     * Este método es responsable de retornar el manager correspondiente.
     *
     * @return Retorna una instancia de FSN.
     */
    public Object getService(Type type) {

        if (!servicesByName.containsKey(getServiceName(type))) {
            try {
                lookupRemoteStatelessEJB(type);
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }
        return this.servicesByName.get(getServiceName(type));
    }

    private String getServiceName(Type type) {
        String[] tokens = type.toString().split(" ")[1].split("\\.");
        return tokens[tokens.length-1]+"Impl";
    }

    private String getViewClassName(Type type) {
        return type.toString().split(" ")[1];
    }

    public void closeContext() {
        try {
            context.close();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

}