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
package org.simexplorer.ui.ide.workflow.model;

import java.util.logging.Logger;
import org.simexplorer.core.workflow.model.metada.Metadata;

/**
 *
 * @author Nicolas Dumoulin <nicolas.dumoulin@cemagref.fr>
 */
public class MetadataProxy {

    public static final String NOT_YET_IMPLEMENTED_WARNING_MESSAGE = "Metadata are not yet handled. See http://www.simexplorer.org/ticket/216.";
    private static final Logger LOGGER = Logger.getLogger(MetadataProxy.class.getName());

    public static String getDescription(Object handler) {
        throw new UnsupportedOperationException(MetadataProxy.NOT_YET_IMPLEMENTED_WARNING_MESSAGE);
    }

    public static void setDescription(Object handler, String description) {
        throw new UnsupportedOperationException(MetadataProxy.NOT_YET_IMPLEMENTED_WARNING_MESSAGE);
    }

    public static Metadata getMetadata(Object handler) {
        // access isn't critical, so we can return empty metadata without regrets
        LOGGER.warning(NOT_YET_IMPLEMENTED_WARNING_MESSAGE);
        return new Metadata();
    }

    public static String getMetadata(Object handler, String key) {
        if (handler instanceof PrototypeWithMetadata) {
            return ((PrototypeWithMetadata) handler).getMetadata(key);
        } else {
            LOGGER.warning(NOT_YET_IMPLEMENTED_WARNING_MESSAGE);
            return "";
        }
    }

    public static void setMetadata(Object handler, Metadata metadata) {
        LOGGER.warning(NOT_YET_IMPLEMENTED_WARNING_MESSAGE);
    }

    public static void setMetadata(Object handler, String key, String value) {
        if (handler instanceof PrototypeWithMetadata) {
            ((PrototypeWithMetadata) handler).setMetadata(key, value);
        } else {
            LOGGER.warning(NOT_YET_IMPLEMENTED_WARNING_MESSAGE);
        }
    }
}
