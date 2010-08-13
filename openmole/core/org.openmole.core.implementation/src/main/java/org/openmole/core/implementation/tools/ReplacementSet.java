/*
 *  Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.tools;

import java.util.HashMap;
import java.util.Map;

public class ReplacementSet {
	Map<String, String> replacements = new HashMap<String, String>();

	public String put(String key, String value) {
		return replacements.put(key, value);
	}

	public boolean containsKey(String key) {
		return replacements.containsKey(key);
	}

	public String get(String key) {
		return replacements.get(key);
	}
	
	
}
