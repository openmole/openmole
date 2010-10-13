/*
 *
 *  Copyright (c) 2009, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.simexplorer.ui.ide.is;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.openide.util.Exceptions;
import org.openmole.commons.exception.InternalProcessingError;

public class ReflectUtils {

    /**
     * An interface that describe an operation to be used on annotated accessible
     * object (field or method) during introspection.
     * @param <A> the type of Annotation
     * @param <AO> the type of accessible object (Field, Method, …)
     */
    public static interface AnnotatedAccessibleObjectOperator<A extends Annotation, AO extends AccessibleObject> {

        /**
         * Function called on an annotated accessible object (field or method).
         * @param accessibleObject the accessible object
         * @param object the carrier, so the object in which is defined the accessible object.
         * @param annotation the annotation
         * @throws org.simexplorer.exception.InternalProcessingError
         */
        public void process(AO accessibleObject, Object object, Class<A> annotation) throws InternalProcessingError;
    }

    /**
     * An operator on annotated field suitable for accessing type operation
     * @param <A>
     */
    public abstract static class FieldGetter<A extends Annotation> implements AnnotatedAccessibleObjectOperator<A, Field> {

        @Override
        public void process(Field f, Object object, Class<A> annotation) throws InternalProcessingError {
            boolean access = f.isAccessible();
            f.setAccessible(true);
            try {
                process(f, object, f.getAnnotation(annotation), f.get(object));
            } catch (IllegalArgumentException e) {
                throw new InternalProcessingError(e);
            } catch (IllegalAccessException e) {
                throw new InternalProcessingError(e);
            } finally {
                f.setAccessible(access);
            }
        }

        /**
         * Function called on an annotated field
         * @param f the field
         * @param object the field carrier, so the object in which is defined the field.
         * @param annotation the annotation
         * @param value the value of the field for the current object
         * @throws org.simexplorer.exception.InternalProcessingError
         */
        public abstract void process(Field f, Object object, A annotation, Object value) throws InternalProcessingError;
    }

    /**
     * An operator on annotated field suitable for setting new values
     * @param <A>
     */
    public abstract static class FieldSetter<A extends Annotation> implements AnnotatedAccessibleObjectOperator<A, Field> {

        private int counter = 0;

        @Override
        public void process(Field f, Object object, Class<A> annotation) throws InternalProcessingError {
            boolean access = f.isAccessible();
            f.setAccessible(true);
            try {
                f.set(object, getValueToSet(f, object, f.getAnnotation(annotation), counter));
            } catch (IllegalAccessException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalArgumentException e) {
                throw new InternalProcessingError(e);
            } finally {
                counter++;
                f.setAccessible(access);
            }
        }

        /**
         * Function called on an annotated field to set the new value
         * @param f the field
         * @param object the field carrier, so the object in which is defined the field.
         * @param annotation the annotation
         * @param counter indicates the position of this field during the operator life-cycle
         * @return the value to set in the field
         * @throws org.simexplorer.exception.InternalProcessingError
         */
        public abstract Object getValueToSet(Field f, Object object, A annotation, int counter) throws InternalProcessingError;
    }

    /**
     * An operator on annotated method suitable for accessing type operation
     * @param <A>
     */
    public abstract static class MethodGetter<A extends Annotation> implements AnnotatedAccessibleObjectOperator<A, Method> {

        @Override
        public void process(Method m, Object object, Class<A> annotation) throws InternalProcessingError {
            // we process only method with no arguments
            if (m.getParameterTypes().length == 0) {
                boolean access = m.isAccessible();
                m.setAccessible(true);
                try {
                    process(m, object, m.getAnnotation(annotation), m.invoke(object));
                } catch (IllegalAccessException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IllegalArgumentException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                } finally {
                    m.setAccessible(access);
                }
            }
        }

        /**
         * Function called on an annotated method
         * @param m the method
         * @param object the method carrier, so the object in which is defined the method.
         * @param annotation the annotation
         * @param value the value of the method return
         * @throws org.simexplorer.exception.InternalProcessingError
         */
        public abstract void process(Method m, Object object, A annotation, Object value) throws InternalProcessingError;
    }

    /**
     * An operator on annotated method suitable for setting new values
     * @param <A>
     */
    public abstract static class MethodSetter<A extends Annotation> implements AnnotatedAccessibleObjectOperator<A, Method> {

        private int counter = 0;

        @Override
        public void process(Method m, Object object, Class<A> annotation) throws InternalProcessingError {
            // we process only method with one arguments
            if (m.getParameterTypes().length == 1) {
                boolean access = m.isAccessible();
                m.setAccessible(true);
                try {
                    m.invoke(object, getValueToSet(m, object, m.getAnnotation(annotation), counter));
                } catch (IllegalAccessException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IllegalArgumentException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                } finally {
                    counter++;
                    m.setAccessible(access);
                }
            }
        }

        /**
         * Function called on an annotated method to set the new value
         * @param m the method
         * @param object the method carrier, so the object in which is defined the method.
         * @param annotation the annotation
         * @param counter indicates the position of this method during the operator life-cycle
         * @return the value to pass to the method
         * @throws org.simexplorer.exception.InternalProcessingError
         */
        public abstract Object getValueToSet(Method m, Object object, A annotation, int counter) throws InternalProcessingError;
    }

    /**
     * Apply an operation on all annotated fields of an object.
     * @param <A> the type of annotation
     * @param object the object treated
     * @param annotation the annotation you are looking for
     * @param operator the operator used
     * @throws org.simexplorer.exception.InternalProcessingError
     */
    public static <A extends Annotation> void processAllTaggedField(Object object, Class<A> annotation, AnnotatedAccessibleObjectOperator<A, Field> operator) throws InternalProcessingError {
        Class cur = object.getClass();
        while (cur != null) {
            for (Field f : cur.getDeclaredFields()) {
                if (f.isAnnotationPresent(annotation)) {
                    operator.process(f, object, annotation);
                }
            }
            cur = cur.getSuperclass();
        }
    }

    /**
     * Apply an operation on all annotated methods of an object.
     * @param <A> the type of annotation
     * @param object the object treated
     * @param annotation the annotation you are looking for
     * @param operator the operator used
     * @throws org.simexplorer.exception.InternalProcessingError
     */
    public static <A extends Annotation> void processAllTaggedMethod(Object object, Class<A> annotation, AnnotatedAccessibleObjectOperator<A, Method> operator) throws InternalProcessingError {
        Class cur = object.getClass();
        while (cur != null) {
            for (Method m : cur.getDeclaredMethods()) {
                if (m.isAnnotationPresent(annotation)) {
                    operator.process(m, object, annotation);
                }
            }
            cur = cur.getSuperclass();
        }
    }
}
