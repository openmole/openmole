package org.simexplorer.ui.ide.workflow.model;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.openide.nodes.Node.Property;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.simexplorer.core.workflow.model.metada.Metadata;

/**
 *
 * @author thierry
 */
    public class MetadataProperties  {
        private Metadata metadata;
        private Sheet.Set set2;
        List<String> reservedKeywords=new ArrayList<String>(Arrays.asList(new String[]{"help"})); // help is used to show help files

        public MetadataProperties(Metadata metadata){
            this.metadata=metadata;
        set2 = Sheet.createPropertiesSet();
        set2.setDisplayName("MetaData");
        set2.setName("metadata");
        }


        public Sheet.Set getProperties() {
            ArrayList<Property> propertys=new ArrayList<Property>();
            for (final Entry<String, String> md : metadata.entrySet()) {
                if (!reservedKeywords.contains(md.getKey())) {
                    set2.put(new PropertySupport.ReadWrite(md.getKey(), String.class, md.getKey(), md.getKey()) {

                        @Override
                        public Object getValue() throws IllegalAccessException, InvocationTargetException {
                            return md.getValue();
                        }

                        @Override
                        public void setValue(Object metadata) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                            md.setValue((String) metadata);
                        }
                    });
                }
            }
        return set2;
        }


    }

