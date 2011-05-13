/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.exception

class GUIUserBadDataError (exception: Throwable, message: String) extends Exception(message, exception) {

    def this (message: String) = {
        this(null, message)
    }

    def this (e: Throwable) {
        this(e, null)
    }
}
