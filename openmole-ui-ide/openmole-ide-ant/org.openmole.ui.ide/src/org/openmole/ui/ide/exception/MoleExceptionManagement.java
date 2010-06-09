/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.exception;

/**
 *
 * @author mathieu
 */
public class MoleExceptionManagement {

    private static MoleExceptionManagement instance;

    public static void showException(Exception e) {
        System.out.println("ShowException " + e + ", to be implemented");
    }

    public static MoleExceptionManagement getInstance() {
        if (instance == null) {
            instance = new MoleExceptionManagement();
        }
        return instance;
    }
}
