/*
 *  Copyright (C) 2010 Cemagref
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.simexplorer.core.workflow.model.metada;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Nicolas Dumoulin <nicolas.dumoulin@cemagref.fr>
 */
public class MetadataLoader {

    /**
     * Build metadata for a given object, by usign its type.
     * @see org.openmole.core.model.metada.MetadataLoader#loadMetada(Class)
     * @param o
     * @return The built metadata container
     */
    public static Metadata loadMetadata(Object o) {
        return loadMetadata(o.getClass());
    }

    /**
     * <p>Build metadata for a given type. First, an XML file is searched to fill
     * these metadata. This XML filename should be the class basename with .xml
     * extension. For example, metadata of the class org.test.MyClass are searched
     * in the file org.test.MyClass.xml.</p>
     * <p>If no XML file is found, a default metadata handler with the simple name
     * of the class is returned.</p>
     * @param type The type to use for building the metadata
     * @return The built metadata container
     */
    public static Metadata loadMetadata(Class type) {
        Metadata metadata = new Metadata();
        try {
            // open document
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    type.getResourceAsStream(type.getSimpleName() + ".xml"));
            // fetch children list
            NodeList nodes = document.getDocumentElement().getChildNodes();
            // browse these children
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    // if element, we put data in the metadata structure
                    Element element = (Element) node;
                    metadata.set(element.getTagName(), element.getTextContent());
                }
            }
            return metadata;

        } catch (Exception e) {
        	Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.FINE, "Metada not found for the class " + type, e);
        // TODO cache metadatas for unknown objects
        }
        Metadata result = new Metadata();
        result.set("name", type.getSimpleName());
        return result;
    }
}
