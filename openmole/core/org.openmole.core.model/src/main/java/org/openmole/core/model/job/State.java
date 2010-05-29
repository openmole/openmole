/*
 *  Copyright (C) 2010 Romain Reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
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

package org.openmole.core.model.job;

public enum State {
	
	READY("Ready"),
	RUNNING("Running"),
        ACHIEVED("Achieved"),
	COMPLETED("Completed"),
	FAILED("Failed"),
        TRANSITION_PERFORMED("Transition Performed"),
        CANCELED("Canceled");
	
	private String label;

	private State(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

        public boolean isFinal() {
            return this == COMPLETED || this == FAILED || this == TRANSITION_PERFORMED || this == CANCELED;
        }
	

}
