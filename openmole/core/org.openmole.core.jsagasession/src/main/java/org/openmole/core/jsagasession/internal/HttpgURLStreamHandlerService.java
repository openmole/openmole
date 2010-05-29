package org.openmole.core.jsagasession.internal;

/*****************************************************************************
 * Copyright (c) 2006, 2007 g-Eclipse Consortium 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial development of the original code was made for the
 * g-Eclipse project founded by European Union
 * project number: FP6-IST-034327  http://www.geclipse.eu/
 *
 * Contributors:
 *    Mateusz Pabis (PSNC) - initial API and implementation
 *****************************************************************************/


import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.globus.net.GSIHttpURLConnection;
import org.osgi.service.url.AbstractURLStreamHandlerService;

class HttpgURLStreamHandlerService
  extends AbstractURLStreamHandlerService {
  
  @Override  
  public URLConnection openConnection( final URL url ) throws IOException {
    return new GSIHttpURLConnection( url );
  }
}
