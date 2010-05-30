package org.openmole.core.implementation.tools;

import java.io.IOException;

import com.rits.cloning.Cloner;
import com.rits.cloning.IFastCloner;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.MultipleException;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.data.Variable;
import org.openmole.core.implementation.internal.Activator;
import org.openmole.core.model.data.IVariable;


import static org.openmole.commons.tools.io.FileUtil.*;

public class ClonningService {

    public static IVariable clone(IVariable variable) throws InternalProcessingError, UserBadDataError {
        if (variable.getValue() == null || variable.getValue().getClass().isPrimitive()) {
            return variable;
        }

        Cloner cloner = new Cloner();
        final List<Throwable> exceptions = Collections.synchronizedList(new LinkedList<Throwable>());

        cloner.registerFastCloner(File.class, new IFastCloner() {

            @Override
            public Object clone(Object o, Cloner cloner, Map<Object, Object> map) throws IllegalAccessException {
                File toClone = (File) o;
                File cloned;
                try {
                    if (toClone.isDirectory()) {
                        cloned = Activator.getWorkspace().newTmpDir();
                    } else {
                        cloned = Activator.getWorkspace().newTmpFile();
                    }

                    copy(toClone, cloned);
                    return cloned;
                } catch (IOException ex) {
                    exceptions.add(ex);
                } catch (InternalProcessingError ex) {
                    exceptions.add(ex);
                }
                return null;
            }
        });

        Object val = cloner.deepClone(variable.getValue());

        if(!exceptions.isEmpty()) {
            throw new InternalProcessingError(new MultipleException(exceptions));
        }

        return new Variable(variable.getPrototype(), val);

       /* try {

            Iterable<File> files = FileMigrator.extractFilesFromVariable(variable);

            LocalFileCache localFileCache = new LocalFileCache();

            for (File f : files) {
                File toClone = f;
                File cloned;

                if (toClone.isDirectory()) {
                    cloned = Activator.getWorkspace().newTmpDir();
                } else {
                    cloned = Activator.getWorkspace().newTmpFile();
                }

                copy(toClone, cloned);
                localFileCache.fillInLocalFileCache(f, cloned);
            }

            IVariable ret = new Variable(variable.getPrototype(), cloner.deepClone(variable.getValue()));
            FileMigrator.initFilesInVariable(ret, localFileCache);

            return ret;

        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        } catch (StackOverflowError error) {
            throw new InternalProcessingError("Unable to clonne variable " + variable.getPrototype() + " , it is to big for the library.");
        }*/
    }
}
