package org.openmole.misc.workspace;

import java.io.File;
import java.io.IOException;

import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;

public interface IWorkspace {
	static final String ConfigurationFile = ".preferences";
	static final String GlobalGroup = "Global";
	static final String DefaultObjectRepoLocaltion = ".objectRepository.bin";
	static final String DefaultTmpLocation = ".tmp";
	
	static final ConfigurationLocation UniqueID = new ConfigurationLocation(GlobalGroup, "UniqueID");
	static final ConfigurationLocation ObjectRepoLocation = new ConfigurationLocation(GlobalGroup, "ObjectRepoLocation");
	static final ConfigurationLocation TmpLocation = new ConfigurationLocation(GlobalGroup, "TmpLocation");

	void setLocation(File location);
        File getLocation();

	File newTmpDir(String prefix) throws IOException, InternalProcessingError;
        File newTmpDir() throws IOException, InternalProcessingError;
	File newTmpFile(String prefix, String suffix) throws IOException, InternalProcessingError;
	File newFile(String prefix, String suffix) throws IOException, InternalProcessingError;
        File newTmpFile() throws IOException, InternalProcessingError;
        File newFile() throws IOException, InternalProcessingError;
      //  File getTmpFile(String name) throws IOException, InternalProcessingError;

        File getFile(String name) throws IOException;

	//String getPreference(String group, String name) throws InternalProcessingError;
        String getPreference(ConfigurationLocation location) throws InternalProcessingError;

       // Integer getPreferenceAsInt(String group, String name) throws InternalProcessingError;
        Integer getPreferenceAsInt(ConfigurationLocation location) throws InternalProcessingError;

        //Long getPreferenceAsLong(String group, String name) throws InternalProcessingError;
        Long getPreferenceAsLong(ConfigurationLocation location) throws InternalProcessingError;

        //Double getPreferenceAsDouble(String group, String name) throws InternalProcessingError;
        Double getPreferenceAsDouble(ConfigurationLocation location) throws InternalProcessingError;

	//void setPreference(String group, String name, String value) throws InternalProcessingError;
	void setPreference(ConfigurationLocation configurationLocation, String value) throws InternalProcessingError;

        //void removePreference(String group, String name) throws InternalProcessingError;
	void removePreference(ConfigurationLocation configurationElement) throws InternalProcessingError;

        void providePassword(String password) throws InternalProcessingError, UserBadDataError;
        void setPasswordProvider(IPasswordProvider passwordProvider);
        void resetPreferences() throws InternalProcessingError;

	void addToConfigurations(ConfigurationLocation location, ConfigurationElement element);
	void addToConfigurations(ConfigurationLocation location, String defaultValue);
	//void addToConfigurations(String group, String location, String defaultValue);

        String getDefaultValue(ConfigurationLocation location);
}
