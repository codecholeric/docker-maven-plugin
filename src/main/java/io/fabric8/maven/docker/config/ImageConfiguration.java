package io.fabric8.maven.docker.config;

import java.util.*;

import io.fabric8.maven.docker.util.EnvUtil;
import io.fabric8.maven.docker.util.Logger;
import io.fabric8.maven.docker.util.StartOrderResolver;

/**
 * @author roland
 * @since 02.09.14
 */
public class ImageConfiguration implements StartOrderResolver.Resolvable {

    /**
     * @parameter
     * @required
     */
    private String name;

    /**
     * @parameter
     */
    private String alias;

    /**
     * @parameter
     */
    private RunImageConfiguration run;

    /**
     * @parameter
     */
    private BuildImageConfiguration build;

    /**
     * @parameter
     */
    private WatchImageConfiguration watch;

    /**
     * @parameter
     */
    private Map<String,String> external;

    /**
     * @parameter
     */
    private String registry;
    
    // Used for injection
    public ImageConfiguration() {}
   
    @Override
    public String getName() {
        return name;
    }

    @Override
	public String getAlias() {
        return alias;
    }

    public RunImageConfiguration getRunConfiguration() {
        return (run == null) ? RunImageConfiguration.DEFAULT : run;
    }

    public BuildImageConfiguration getBuildConfiguration() {
        return build;
    }

    public WatchImageConfiguration getWatchConfiguration() {
        return watch;
    }

    public Map<String, String> getExternalConfig() {
        return external;
    }

    @Override
    public List<String> getDependencies() {
        RunImageConfiguration runConfig = getRunConfiguration();
        List<String> ret = new ArrayList<>();
        if (runConfig != null) {
            addVolumes(runConfig, ret);
            addLinks(runConfig, ret);
            addContainerNetwork(runConfig, ret);
        }
        return ret;
    }

    private void addVolumes(RunImageConfiguration runConfig, List<String> ret) {
        VolumeConfiguration volConfig = runConfig.getVolumeConfiguration();
        if (volConfig != null) {
            List<String> volumeImages = volConfig.getFrom();
            if (volumeImages != null) {
                ret.addAll(volumeImages);
            }
        }
    }

    private void addLinks(RunImageConfiguration runConfig, List<String> ret) {
        // Custom networks can have circular links, no need to be considered for the starting order.
        if (runConfig.getLinks() != null && !runConfig.getNetworkingMode().isCustomNetwork()) {
            for (String[] link : EnvUtil.splitOnLastColon(runConfig.getLinks())) {
                ret.add(link[0]);
            }
        }
    }

    private void addContainerNetwork(RunImageConfiguration runConfig, List<String> ret) {
        NetworkingMode mode = runConfig.getNetworkingMode();
        String alias = mode.getContainerAlias();
        if (alias != null) {
            ret.add(alias);
        }
    }

    public boolean isDataImage() {
        // If there is no explicit run configuration, its a data image
        // TODO: Probably add an explicit property so that a user can indicated whether it
        // is a data image or not on its own.
        return getRunConfiguration() == null;
    }
    
    public String getDescription() {
        return String.format("[%s] %s", name, (alias != null ? "\"" + alias + "\"" : ""));
    }

    public String getRegistry() {
        return registry;
    }

    @Override
    public String toString() {
        return String.format("ImageConfiguration {name='%s', alias='%s'}", name, alias);
    }

    public String validate(Logger log) {
        String minimalApiVersion = null;
        if (null != build) {
            minimalApiVersion = build.validate(log);
        }
        if (null != run) {
            minimalApiVersion = EnvUtil.extractLargerVersion(minimalApiVersion,run.validate());
        }
        return minimalApiVersion;
    }

    // =========================================================================
    // Builder for image configurations

    public static class Builder {
        private ImageConfiguration config = new ImageConfiguration();

        public Builder name(String name) {
            config.name = name;
            return this;
        }

        public Builder alias(String alias) {
            config.alias = alias;
            return this;
        }

        public Builder runConfig(RunImageConfiguration runConfig) {
            config.run = runConfig;
            return this;
        }

        public Builder buildConfig(BuildImageConfiguration buildConfig) {
            config.build = buildConfig;
            return this;
        }

        public Builder externalConfig(Map<String, String> externalConfig) {
            config.external = externalConfig;
            return this;
        }
        
        public ImageConfiguration build() {
            return config;
        }

        public Builder watchConfig(WatchImageConfiguration watchConfig) {
            config.watch = watchConfig;
            return this;
        }
    }
}
