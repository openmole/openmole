package org.openmole.misc.workspace.internal;


import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;


import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.util.text.BasicTextEncryptor;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.openmole.commons.aspect.caching.Cachable;
import org.openmole.commons.aspect.caching.ChangeState;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.misc.workspace.ConfigurationElement;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.misc.workspace.IPasswordProvider;
import org.openmole.misc.workspace.IWorkspace;

public class Workspace implements IWorkspace {

    static final String group = Workspace.class.getSimpleName();
    static final String fixedPrefix = "file";
    static final String fixedPostfix = ".wf";
    static final String fixedDir = "dir";
    static final ConfigurationLocation passwordTest = new ConfigurationLocation(group, "passwordTest", true);
    static final String passwordTestString = "test";
    File location;
    Map<ConfigurationLocation, ConfigurationElement> configurations = new TreeMap<ConfigurationLocation, ConfigurationElement>();
    transient IPasswordProvider passwordProvider;
    transient long currentTime = System.currentTimeMillis();


    Workspace(File location) {
        this.location = location;
        /*if (!exist()) {
        create();
        }*/
        addToConfigurations(UniqueID, new ConfigurationElement() {

            @Override
            public String getDefaultValue() {
                return UUID.randomUUID().toString();
            }
        });
        addToConfigurations(ObjectRepoLocation, new ConfigurationElement() {

            @Override
            public String getDefaultValue() {
                return new File(getLocation(), DefaultObjectRepoLocaltion).getAbsolutePath();
            }
        });
        addToConfigurations(TmpLocation, new ConfigurationElement() {

            @Override
            public String getDefaultValue() {
                return new File(getLocation(), DefaultTmpLocation).getAbsolutePath();
            }
        });

        addToConfigurations(passwordTest, passwordTestString);
    }

    @Override
    public synchronized File getLocation() {
        if (!location.exists()) {
            location.mkdirs();
        }
        return location;
    }

    @Override
    public synchronized void addToConfigurations(ConfigurationLocation location, ConfigurationElement element) {
        configurations.put(location, element);
    }

    @Override
    public synchronized void addToConfigurations(ConfigurationLocation location, String element) {
        configurations.put(location, new ConfigurationElement(element));
    }

    @ChangeState
    @Override
    public synchronized void setLocation(File location) {
        this.location = location;
    }

    @Cachable
    synchronized TempDir getTmpDir() throws IOException, InternalProcessingError {
        File locTmp = new File(getPreference(TmpLocation), getPreference(UniqueID));
        locTmp = new File(locTmp, Long.toString(currentTime));

        if (!locTmp.exists()) {
            if (!locTmp.mkdirs()) {
                throw new IOException("Cannot create tmp dir " + locTmp.getAbsolutePath());
            }
        }

        locTmp.deleteOnExit();
        return new TempDir(locTmp);
    }


    @Override
    public File newTmpDir(String prefix) throws IOException, InternalProcessingError {
        return getTmpDir().createNewTempDir(prefix);
    }

    @Override
    public File newTmpFile(String prefix, String suffix) throws IOException, InternalProcessingError {
        return getTmpDir().createNewTempFile(prefix, suffix);
    }

    @Override
    public File newFile(String prefix, String suffix) throws IOException, InternalProcessingError {
        return getTmpDir().createNewFile(prefix, suffix);
    }

    public File getTmpFile(String name) throws IOException, InternalProcessingError {
        File ret = new File(getTmpDir().getLocation(), name);
        ret.deleteOnExit();
        return ret;
    }

    @Cachable
    private File getConfigurationFile() throws InternalProcessingError {
        File configurationCacheTmp;
        configurationCacheTmp = new File(getLocation(), ConfigurationFile);

        try {
            configurationCacheTmp.createNewFile();
        } catch (IOException e) {
            throw new InternalProcessingError(e, "Error creating the configuration file " + configurationCacheTmp.getAbsolutePath());
        }

        return configurationCacheTmp;
    }

    @Cachable
    private synchronized FileConfiguration getConfiguration() throws InternalProcessingError {
        try {
            FileConfiguration configuration = new PropertiesConfiguration(getConfigurationFile());
            configuration.setReloadingStrategy(new FileChangedReloadingStrategy());
            return configuration;
        } catch (ConfigurationException e) {
            throw new InternalProcessingError(e);
        }
    }


    @Override
    public String getDefaultValue(ConfigurationLocation location) {
        ConfigurationElement cf = configurations.get(location);
        if (cf == null) {
            return "";
        }
        String ret = cf.getDefaultValue();
        if (ret == null) {
            return "";
        }
        return ret;
    }

    @Override
    public synchronized String getPreference(ConfigurationLocation location) throws InternalProcessingError {
        Configuration conf = getConfiguration().subset(location.getGroup());
        String ret = conf.getString(location.getName());

        if (ret == null && configurations.containsKey(location)) {
            ret = configurations.get(location).getDefaultValue();
            setPreference(location, ret);
            return ret;
        }

        if (!location.isCyphered()) {
            return ret;
        } else {
            return getTextEncryptor().decrypt(ret);
        }

    }

    @Override
    public synchronized void setPreference(ConfigurationLocation location, String value) throws InternalProcessingError {
        try {
            Configuration conf = getConfiguration().subset(location.getGroup());
            String prop = location.isCyphered() ? getTextEncryptor().encrypt(value) : value;
            conf.setProperty(location.getName(), prop);
            getConfiguration().save();
        } catch (ConfigurationException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    @Cachable
    private synchronized BasicTextEncryptor getTextEncryptor() throws InternalProcessingError {
        // if (textEncryptor == null) {
        String password = getPasswordProvider().getPassword();
        BasicTextEncryptor tmpTextEncryptor = new BasicTextEncryptor();
        tmpTextEncryptor.setPassword(password);
        //textEncryptor = tmpTextEncryptor;
        //  }

        return tmpTextEncryptor;
    }

    /*private synchronized void reinitTextEncryptor() {
    textEncryptor = null;
    }*/
    @Override
    public void setPasswordProvider(IPasswordProvider passwordProvider) {
        this.passwordProvider = passwordProvider;
    }

    private IPasswordProvider getPasswordProvider() {
        if (passwordProvider != null) {
            return passwordProvider;
        }

        synchronized (this) {
            if (passwordProvider == null) {
                passwordProvider = new SystemInPasswordProvider();
            }
            return passwordProvider;
        }
    }

    @Override
    public synchronized void removePreference(
            ConfigurationLocation location)
            throws InternalProcessingError {
        Configuration conf = getConfiguration().subset(location.getGroup());
        conf.clearProperty(location.getName());

    }

    @Override
    public File getFile(String name) throws IOException {
        return new File(getLocation(), name);
    }

    @Override
    public int getPreferenceAsInt(ConfigurationLocation location) throws InternalProcessingError {
        return new Integer(getPreference(location));
    }

    @Override
    public double getPreferenceAsDouble(ConfigurationLocation location) throws InternalProcessingError {
        return new Double(getPreference(location));
    }

    @Override
    public File newTmpFile() throws IOException, InternalProcessingError {
        return newTmpFile(fixedPrefix, fixedPrefix);
    }

    @Override
    public File newFile() throws IOException, InternalProcessingError {
        return newFile(fixedPrefix, fixedPostfix);
    }

    @Override
    public File newTmpDir() throws IOException, InternalProcessingError {
        return newTmpDir(fixedDir);
    }

    @ChangeState
    @Override
    public synchronized void resetPreferences() throws InternalProcessingError {
        getConfigurationFile().delete();
    }

    @Override
    public long getPreferenceAsLong(ConfigurationLocation location) throws InternalProcessingError {
        return new Long(getPreference(location));
    }

    @Override
    public void providePassword(String password) throws InternalProcessingError, UserBadDataError {
        setPasswordProvider(new StaticPasswordProvider(password));
        //reinitTextEncryptor();
        try {
            getPreference(passwordTest);
        } catch (EncryptionOperationNotPossibleException e) {
            throw new UserBadDataError(e, "Wrong password");
        }
    }


    @Override
    public long getPreferenceAsDurationInMs(ConfigurationLocation location) throws InternalProcessingError {
        return ISOPeriodFormat.standard().parsePeriod(getPreference(location)).toStandardSeconds().getSeconds() * 1000L;
    }

    @Override
    public int getPreferenceAsDurationInS(ConfigurationLocation location) throws InternalProcessingError {
        PeriodFormatter formatter = ISOPeriodFormat.standard();
        Period period = formatter.parsePeriod(getPreference(location));
        return period.toStandardSeconds().getSeconds();
    }
}
