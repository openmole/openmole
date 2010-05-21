package org.openmole.misc.workspace;

import java.io.IOException;

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
