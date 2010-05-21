package org.openmole.misc.workspace;

public class ConfigurationLocation implements Comparable<ConfigurationLocation>{

    String group;
    String name;
    boolean cyphered;

    public ConfigurationLocation(String group, String name) {
        this(group, name, false);
    }

    public ConfigurationLocation(String group, String name, boolean cyphered) {
        super();
        this.group = group;
        this.name = name;
        this.cyphered = cyphered;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public boolean isCyphered() {
        return cyphered;
    }

    @Override
    public int compareTo(ConfigurationLocation o) {
        int compare = getGroup().compareTo(o.getGroup());
        if(compare != 0) return compare;

        return getName().compareTo(o.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConfigurationLocation other = (ConfigurationLocation) obj;
        if ((this.group == null) ? (other.group != null) : !this.group.equals(other.group)) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.group != null ? this.group.hashCode() : 0);
        hash = 71 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

   
}
