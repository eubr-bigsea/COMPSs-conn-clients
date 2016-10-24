package es.bsc.conn.jclouds.client;

import static com.google.common.base.Predicates.not;
import static com.google.common.base.Charsets.UTF_8;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jclouds.ContextBuilder;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.Location;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.googlecloud.GoogleCredentialsFromJson;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;
import org.jclouds.sshj.config.SshjSshClientModule;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Module;

import es.bsc.conn.loggers.Loggers;


public class JCloudsClient {
    
    private static final Logger LOGGER = LogManager.getLogger(Loggers.JCLOUDS);

    public static final Map<String, ProviderMetadata> appProviders = Maps.uniqueIndex(Providers.viewableAs(ComputeServiceContext.class),
            Providers.idFunction());
    public static final Map<String, ApiMetadata> apiProviders = Maps.uniqueIndex(Apis.viewableAs(ComputeServiceContext.class),
            Apis.idFunction());
    public static final Set<String> allKeys = ImmutableSet.copyOf(Iterables.concat(appProviders.keySet(), apiProviders.keySet()));

    private ComputeServiceContext context;
    ComputeService computeService;


    public JCloudsClient(String user, String credential, String provider, String endpoint) throws Exception {
        LOGGER.info("Creating JClouds Client");
        if (!allKeys.contains(provider)) {
            throw new Exception("Provider is not in the list. Available providers are: " + allKeys);
        }
        ContextBuilder contextBuilder = ContextBuilder.newBuilder(provider);
        if (endpoint != null && !endpoint.isEmpty()) {
            contextBuilder = contextBuilder.endpoint(endpoint);
        }
        String credentialStr = extractCredential(credential);
        context = contextBuilder.credentials(user, credentialStr)
                .modules(ImmutableSet.<Module> of(new Log4JLoggingModule(), new SshjSshClientModule()))
                .buildView(ComputeServiceContext.class);

        computeService = context.getComputeService();

    }

    private String extractCredential(String credential) {
        File f = new File(credential);
        if (f.exists()) {
            // Is file
            if (credential.endsWith(".json")) {
                return getCredentialFromJsonKeyFile(credential);
            } else {
                return credential;
            }
        } else {
            return credential;
        }
    }

    private static String getCredentialFromJsonKeyFile(String filename) {
        try {
            String fileContents = Files.toString(new File(filename), UTF_8);
            Supplier<Credentials> credentialSupplier = new GoogleCredentialsFromJson(fileContents);
            String credential = credentialSupplier.get().credential;
            return credential;
        } catch (IOException e) {
            LOGGER.error("Exception reading private key from '%s': " + filename, e);
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    public Set<? extends NodeMetadata> createVMS(String groupName, int numVMs, Template template) throws RunNodesException {
        return computeService.createNodesInGroup(groupName, numVMs, template);
    }

    public Set<? extends Location> getLocations() {
        return computeService.listAssignableLocations();
    }

    public Set<? extends Image> getImages() {
        return computeService.listImages();
    }

    public Set<? extends Hardware> getHardwareProfiles() {
        return computeService.listHardwareProfiles();

    }

    public Set<? extends ComputeMetadata> getNodes() {
        return computeService.listNodes();
    }

    public Template createTemplate(String hwID, String imageID, TemplateOptions options) {
        TemplateBuilder templateBuilder = computeService.templateBuilder();
        if (hwID != null) {
            templateBuilder.hardwareId(hwID);
        }
        if (imageID != null) {
            templateBuilder.imageId(imageID);
        }
        if (options != null) {
            templateBuilder.options(options);
        }
        return templateBuilder.build();
    }

    public Template createTemplate(Double minCores, Double minDisk, Integer minRam, String imageID, TemplateOptions options) {
        TemplateBuilder templateBuilder = computeService.templateBuilder();
        if (minCores != null) {
            templateBuilder.minCores(minCores);
        }
        if (minDisk != null) {
            templateBuilder.minDisk(minDisk);
        }
        if (minRam != null) {
            templateBuilder.minRam(minRam);
        }
        if (imageID != null) {
            templateBuilder.imageId(imageID);
        }
        if (options != null) {
            templateBuilder.options(options);
        }
        return templateBuilder.build();
    }

    public Template createTemplate(Double minCores, Double minDisk, Integer minRam, TemplateOptions options, Predicate<Image> arg0) {
        TemplateBuilder templateBuilder = computeService.templateBuilder();
        if (minCores != null) {
            templateBuilder.minCores(minCores);
        }
        if (minDisk != null) {
            templateBuilder.minDisk(minDisk);
        }
        if (minRam != null) {
            templateBuilder.minRam(minRam);
        }
        templateBuilder.imageMatches(arg0);
        if (options != null) {
            templateBuilder.options(options);
        }
        return templateBuilder.build();
    }

    public Set<? extends NodeMetadata> destroyAllNodesInGroup(String groupName) {
        return computeService.destroyNodesMatching(//
                Predicates.<NodeMetadata> and(not(TERMINATED), inGroup(groupName)));
    }

    public void destroyNode(String nodeID) {
        computeService.destroyNode(nodeID);
    }

    public static String getCredentialFromFile(String filename) throws IOException {
        return Files.toString(new File(filename), Charsets.UTF_8);

    }

    public static LoginCredentials getCurrentUserLoginForCommandExecution() throws IOException {
        String user = System.getProperty("user.name");
        String privateKey = Files.toString(new File(System.getProperty("user.home") + "/.ssh/id_rsa"), Charsets.UTF_8);
        return LoginCredentials.builder().user(user).privateKey(privateKey).build();
    }

    public NodeMetadata getNode(String id) {
        return computeService.getNodeMetadata(id);
    }

}
