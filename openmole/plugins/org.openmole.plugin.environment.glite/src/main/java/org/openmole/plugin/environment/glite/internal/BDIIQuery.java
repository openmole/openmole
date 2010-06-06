/******************************************************************************
 * Copyright (c) 2008 g-Eclipse consortium
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial development of the original code was made for
 * project g-Eclipse founded by European Union
 * project number: FP6-IST-034327  http://www.geclipse.eu/
 *
 * Contributor(s):
 *    Nikolaos Tsioutsias - implementation
 *****************************************************************************/
package org.openmole.plugin.environment.glite.internal;

import java.util.ArrayList;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.openmole.commons.exception.InternalProcessingError;



/**
 * @author tnikos
 *
 */
public class BDIIQuery {
  
  private String bdii; 
  private ArrayList<String> attributeList = new ArrayList<String>();
  /**
   * @param bdii the end poing of the bdii to query
   */
  public BDIIQuery(final String bdii)
  {
    this.bdii = bdii;
  }
  
  /**
   * Sets the specific attributes of the element that we want to return
   * @param attribute a valid 
   */
  public void setAttribute(final String attribute)
  {
    if (attribute!=null && attribute.length() > 0 )
    {
      this.attributeList.add( attribute );
    }
  }
  
  private ArrayList<String> getAttributes()
  {
    return this.attributeList;
  }
  /**
   * This method queries the bdii set in the constructor
   * @param searchPhrase the search phrase
   * @return an array list of SearchResult objects.
 * @throws InternalProcessingError 
   */
  public ArrayList<SearchResult> query(final String searchPhrase, int timeOut) throws InternalProcessingError
  {
    boolean hasError= false;
    ArrayList<SearchResult> resultsList = new ArrayList<SearchResult>();
    String bindDN = "o=grid";
    Hashtable<String, String> env = new Hashtable<String, String>();
    
    env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
    env.put( Context.PROVIDER_URL, this.bdii );
    
    if ( ! hasError ) {
      try {
        /* get a handle to an Initial DirContext */
        DirContext dirContext = new InitialDirContext( env );
        
        /* specify search constraints to search subtree */
        SearchControls constraints = new SearchControls();
        constraints.setTimeLimit(timeOut);
        constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
        
        // specify the elements to return
        if (this.getAttributes().size()> 0)
          constraints.setReturningAttributes( this.getAttributes().toArray( new String[this.getAttributes().size()] ));
        
        // Perform the search
        NamingEnumeration<SearchResult> results = dirContext.search( bindDN,
                                                                     searchPhrase,
                                                                     constraints );
        resultsList = java.util.Collections.list( results );
        
      } catch( NamingException e ) {
        throw new InternalProcessingError(e);
      }
    }
    return resultsList;
  }
}
