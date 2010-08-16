/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.tools;

import org.openmole.commons.exception.InternalProcessingError;

/**
 *
 * Data structure for registring value in fonction of a key. It acts like
 * an hashmap.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public interface IRegistry<K,V> {

    boolean isRegistredFor(K transition);

    void register(K key, V job);

    V consult(K key);

    V removeFromRegistry(K key);
}
