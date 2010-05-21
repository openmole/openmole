/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.misc.caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author reuillon
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ChangeState {

}
