package org.openmole.misc.workspace.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.misc.workspace.IPasswordProvider;


public class SystemInPasswordProvider implements IPasswordProvider {

    static final String message = "Enter the secure storage password: ";
 
    @Override
    public synchronized String getPassword() throws InternalProcessingError {
        String password;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(message);

        try {
            password = in.readLine();
        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }

        return password;
    }

 
}
