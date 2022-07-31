package org.sonatype.nexus.bootstrap.osgi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.FeaturesService.Option;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.bootstrap.internal.DirectoryHelper;

public class BootstrapListener implements ServletContextListener {
    private static final String NEXUS_LOAD_AS_OSS_PROP_NAME = "nexus.loadAsOSS";
    private static final String EDITION_PRO = "edition_pro";
    private static final Logger log = LoggerFactory.getLogger(BootstrapListener.class);
    private static final String NEXUS_EDITION = "nexus-edition";
    private static final String NEXUS_FULL_EDITION = "nexus-full-edition";
    private static final String NEXUS_FEATURES = "nexus-features";
    private static final String NEXUS_PRO_FEATURE = "nexus-pro-feature";
    private static final String NEXUS_OSS_EDITION = "nexus-oss-edition";
    private static final String NEXUS_OSS_FEATURE = "nexus-oss-feature";
    private static final String NEXUS_DB_FEATURE = "nexus-db-feature";
    private static final String NEXUS_EXCLUDE_FEATURES = "nexus-exclude-features";
    private ListenerTracker listenerTracker;
    private FilterTracker filterTracker;

    public BootstrapListener() {
    }

    public void contextInitialized(ServletContextEvent event) {
        log.info("Initializing");
        ServletContext servletContext = event.getServletContext();

        try {
            Properties properties = System.getProperties();
            if (properties == null) {
                throw new IllegalStateException("Missing bootstrap configuration properties");
            }

            requireProperty(properties, "karaf.base");
            requireProperty(properties, "karaf.data");
            File workDir = (new File(properties.getProperty("karaf.data"))).getCanonicalFile();
            Path workDirPath = workDir.toPath();
            DirectoryHelper.mkdir(workDirPath);
            if (this.hasProFeature(properties)) {
                if (this.shouldSwitchToOss(workDirPath)) {
                    this.adjustEditionProperties(properties);
                } else {
                    this.createProEditionMarker(workDirPath);
                }
            }

            selectDatastoreFeature(properties);
            selectAuthenticationFeature(properties);
            servletContext.setAttribute("nexus.properties", properties);
            Bundle containingBundle = FrameworkUtil.getBundle(this.getClass());
            if (containingBundle == null) {
                throw new UnsupportedOperationException("Missing OSGi container");
            }

            BundleContext bundleContext = containingBundle.getBundleContext();
            requireProperty(properties, "nexus-edition");
            requireProperty(properties, "nexus-db-feature");
            installNexusEdition(bundleContext, properties);
            this.listenerTracker = new ListenerTracker(bundleContext, "nexus", servletContext);
            this.listenerTracker.open();
            this.filterTracker = new FilterTracker(bundleContext, "nexus");
            this.filterTracker.open();
            this.listenerTracker.waitForService(0L);
            this.filterTracker.waitForService(0L);
        } catch (Exception var8) {
            log.error("Failed to initialize", var8);
            throw var8 instanceof RuntimeException ? (RuntimeException)var8 : new RuntimeException(var8);
        }

        log.info("Initialized");
    }

    private boolean hasProFeature(Properties properties) {
        return properties.getProperty("nexus-features", "").contains("nexus-pro-feature");
    }

    private void adjustEditionProperties(Properties properties) {
        log.info("Loading OSS Edition");
        properties.put("nexus-edition", "nexus-oss-edition");
        properties.put("nexus-features", properties.getProperty("nexus-features").replace("nexus-pro-feature", "nexus-oss-feature"));
    }

    boolean shouldSwitchToOss(Path workDirPath) {
        return false;
//        File proEditionMarker = this.getProEditionMarker(workDirPath);
//        boolean switchToOss;
//        if (this.hasNexusLoadAsOSS()) {
//            switchToOss = this.isNexusLoadAsOSS();
//        } else if (proEditionMarker.exists()) {
//            switchToOss = false;
//        } else if (this.isNexusClustered()) {
//            switchToOss = false;
//        } else {
//            switchToOss = this.isNullNexusLicenseFile() && this.isNullJavaPrefLicense();
//        }
//
//        return switchToOss;
    }

    boolean hasNexusLoadAsOSS() {
        return System.getProperty("nexus.loadAsOSS") != null;
    }

    boolean isNexusLoadAsOSS() {
        return Boolean.getBoolean("nexus.loadAsOSS");
    }

    File getProEditionMarker(Path workDirPath) {
        return workDirPath.resolve("edition_pro").toFile();
    }

    private void createProEditionMarker(Path workDirPath) {
        File proEditionMarker = this.getProEditionMarker(workDirPath);

        try {
            if (proEditionMarker.createNewFile()) {
                log.debug("Created pro edition marker file: {}", proEditionMarker);
            }
        } catch (IOException var4) {
            log.error("Failed to create pro edition marker file: {}", proEditionMarker, var4);
        }

    }

    boolean isNexusClustered() {
        return Boolean.getBoolean("nexus.clustered");
    }

    boolean isNullNexusLicenseFile() {
        return System.getProperty("nexus.licenseFile") == null;
    }

    boolean isNullJavaPrefLicense() {
        Thread currentThread = Thread.currentThread();
        ClassLoader tccl = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader((ClassLoader)null);

        boolean var4;
        try {
            var4 = Preferences.userRoot().node("/com/sonatype/nexus/professional").get("license", (String)null) == null;
        } finally {
            currentThread.setContextClassLoader(tccl);
        }

        return var4;
    }

    private static void selectDatastoreFeature(Properties properties) {
        if (Boolean.parseBoolean(properties.getProperty("nexus.datastore.developer", "false"))) {
            properties.setProperty("nexus.datastore.enabled", "true");
        }

        if (Boolean.parseBoolean(properties.getProperty("nexus.datastore.clustered.enabled", "false"))) {
            properties.setProperty("nexus.datastore.enabled", "true");
        }

        if (Boolean.parseBoolean(properties.getProperty("nexus.datastore.search.enabled", "false"))) {
            properties.setProperty("nexus.datastore.enabled", "true");
            properties.setProperty("nexus.elasticsearch.enabled", "false");
        }

        if (Boolean.parseBoolean(properties.getProperty("nexus.elasticsearch.enabled", "false"))) {
            properties.setProperty("nexus.datastore.search.enabled", "false");
        }

        if (Boolean.parseBoolean(properties.getProperty("nexus.datastore.enabled", "false"))) {
            properties.setProperty("nexus.orient.enabled", "false");
            if (!Boolean.parseBoolean(properties.getProperty("nexus.datastore.developer", "false"))) {
                properties.setProperty("nexus-exclude-features", properties.getProperty("nexus-exclude-features", ""));
            }
        }

        selectDbFeature(properties);
    }

    private static void selectDbFeature(Properties properties) {
        if (Boolean.parseBoolean(properties.getProperty("nexus.orient.enabled", "true"))) {
            properties.setProperty("nexus-db-feature", "nexus-orient");
            properties.setProperty("nexus.orient.enabled", "true");
        } else {
            properties.setProperty("nexus-db-feature", "nexus-datastore-mybatis");
            properties.setProperty("nexus.datastore.enabled", "true");
            properties.setProperty("nexus.quartz.jobstore.jdbc", "true");
            if ("nexus-oss-edition".equals(properties.getProperty("nexus-edition"))) {
                properties.setProperty("nexus-exclude-features", "nexus-cma-feature," + properties.getProperty("nexus-exclude-features", ""));
            }
        }

    }

    private static void selectAuthenticationFeature(Properties properties) {
        if (Boolean.parseBoolean(properties.getProperty("nexus.session.enabled", "true"))) {
            properties.setProperty("nexus.session.enabled", "true");
        }

        if (Boolean.parseBoolean(properties.getProperty("nexus.jwt.enabled", "false"))) {
            properties.setProperty("nexus.session.enabled", "false");
        }

    }

    private static void installNexusEdition(BundleContext ctx, Properties properties) throws Exception {
        String editionName = properties.getProperty("nexus-edition");
        if (editionName != null && editionName.length() > 0) {
            ServiceTracker<?, FeaturesService> tracker = new ServiceTracker(ctx, FeaturesService.class, (ServiceTrackerCustomizer)null);
            tracker.open();

            try {
                FeaturesService featuresService = (FeaturesService)tracker.waitForService(1000L);
                Feature editionFeature = featuresService.getFeature(editionName);
                properties.put("nexus-full-edition", editionFeature.toString());
                Feature dbFeature = featuresService.getFeature(properties.getProperty("nexus-db-feature"));
                log.info("Installing: {} ({})", editionFeature, dbFeature);
                Set<String> featureIds = new LinkedHashSet();
                if (!featuresService.isInstalled(editionFeature)) {
                    featureIds.add(editionFeature.getId());
                }

                if (!featuresService.isInstalled(dbFeature)) {
                    featureIds.add(dbFeature.getId());
                }

                if (!featureIds.isEmpty()) {
                    EnumSet<FeaturesService.Option> options = EnumSet.of(Option.NoAutoRefreshBundles, Option.NoAutoRefreshManagedBundles);
                    featuresService.installFeatures(featureIds, options);
                }

                log.info("Installed: {} ({})", editionFeature, dbFeature);
            } finally {
                tracker.close();
            }
        }

    }

    private static void requireProperty(Properties properties, String name) {
        if (!properties.containsKey(name)) {
            throw new IllegalStateException("Missing required property: " + name);
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        log.info("Destroying");
        if (this.filterTracker != null) {
            this.filterTracker.close();
            this.filterTracker = null;
        }

        if (this.listenerTracker != null) {
            this.listenerTracker.close();
            this.listenerTracker = null;
        }

        log.info("Destroyed");
    }
}