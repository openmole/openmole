package org.openmole.misc.workspace;

public class ConfigurationElement {

	String defaultValue = "";

	public ConfigurationElement(String defaultValue) {
		super();
		this.defaultValue = defaultValue;
	}

	public ConfigurationElement() {
		super();
	}	

	public String getDefaultValue() {
		return defaultValue;
	}

}
