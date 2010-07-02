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

    /**
     * 
     * The job as been created and is ready to be excuted.
     * 
     */
    READY,
    
    /**
     * 
     * The job is being executed.
     * 
     */
    RUNNING,
    
    /**
     * 
     * The job has sucessfully ended.
     * 
     */
    COMPLETED,
    
    /**
     * 
     * The job has failed, an uncatched exception has been raised
     * to the workflow engine.
     * 
     */
    FAILED,
    
    /**
     * 
     * The job has been canceled.
     * 
     */
    CANCELED;
    
    /** 
     * 
     * Get if the state is a final state. Meaning there is no way it the {@link IMoleJob} state
     * can change again.
     * 
     * @return true if the state is final
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == CANCELED;
    }
}
