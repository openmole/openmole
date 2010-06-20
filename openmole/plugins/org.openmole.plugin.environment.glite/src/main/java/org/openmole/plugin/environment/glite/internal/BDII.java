/*
 *  Copyright (C) 2010 reuillon
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
package org.openmole.plugin.environment.glite.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;

import javax.naming.directory.SearchResult;

import org.openmole.commons.exception.InternalProcessingError;

public class BDII {

    final static String[] srmServiceType = {"srm", "SRM"/*, "srm_v1"*/};
    final static String[] wmsServiceType = {"org.glite.wms.WMProxy"/*, "org.glite.wms"*/};
    final String location;

    public BDII(String location) {
        super();
        this.location = location;
    }

    public Set<URI> querySRMURIs(String vo, int timeOut) throws InternalProcessingError {

        Set<URI> srmURIs = new HashSet<URI>();

        BDIIQuery q = new BDIIQuery(location);

        ArrayList<SearchResult> res = q.query("(&(objectClass=GlueSA)(GlueSAAccessControlBaseRule=VO:" + vo + "))", timeOut);

        Map<String, String> ids = new TreeMap<String, String>();

        for (SearchResult r : res) {

            try {
                // GlueSEAccessProtocol glueSEAccessProtocol = new GlueSEAccessProtocol();
                // this.setRetrievalTime( sdf.format(cal.getTime()) );
                //Attribute a = r.getAttributes().get("GlueChunkKey");
                String id = r.getAttributes().get("GlueChunkKey").get().toString(); //$NON-NLS-1$;
                id = id.substring(id.indexOf('=') + 1);
                ArrayList<SearchResult> resForPath = q.query("(&(GlueChunkKey=GlueSEUniqueID=" + id + ")(GlueVOInfoAccessControlBaseRule=VO:" + vo + "))", timeOut);
                if (!resForPath.isEmpty()) {
                    String path = resForPath.get(0).getAttributes().get("GlueVOInfoPath").get().toString();
                    ids.put(id, path);
                }
                /*
                a = r.getAttributes().get("GlueSAPath");
                System.out.println(a.get().toString());
                /*NamingEnumeration<? extends Attribute> e = r.getAttributes().getAll();
                while (e.hasMoreElements()) {
                Attribute attribute = (Attribute) e.nextElement();
                System.out.println(attribute.getID() + " = " + attribute.get());
                }*/
                //System.out.println(GlueUtility.getStringAttribute( "GlueSEUniqueID", r.getAttributes()));
            } catch (NamingException ex) {
                Logger.getLogger(BDII.class.getName()).log(Level.FINE, "Error when quering BDII", ex);
            }



        }


        String searchPhrase =
                "(&(objectClass=GlueService)(GlueServiceUniqueID=*)(GlueServiceAccessControlRule=" + vo + ")"; //$NON-NLS-1$


        searchPhrase += "(|"; //$NON-NLS-1$
        for (String type : srmServiceType) {
            searchPhrase += "(GlueServiceType=" + type + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        searchPhrase += "))";

        //System.out.println(searchPhrase);

        res = q.query(searchPhrase, timeOut);


        Set<String> srmIds = new TreeSet<String>();

        for (SearchResult r : res) {

            try {
                String serviceEndPoint = r.getAttributes().get("GlueServiceEndpoint").get().toString();


                URI httpgURI = new URI(serviceEndPoint);

                // System.out.println(httpgURI.getHost());
                if (ids.containsKey(httpgURI.getHost())) {

                    StringBuilder srmURI = new StringBuilder();

                    srmURI.append("srm");
                    srmURI.append("://");
                    srmURI.append(httpgURI.getHost());
                    if (httpgURI.getPort() != -1) {
                        srmURI.append(':');
                        srmURI.append(httpgURI.getPort());
                    }

                    //System.out.println();
                    srmURI.append(ids.get(httpgURI.getHost()));

                    String srmURIString = srmURI.toString();
                    srmURIs.add(URI.create(srmURIString));

                    srmIds.add(httpgURI.getHost());
                } else {
                    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.FINE, "No path found in BDII for host " + httpgURI.getHost());
                }



            } catch (NamingException ex) {
                Logger.getLogger(BDII.class.getName()).log(Level.FINE, "Error interrogating the BDII.", ex);
            } catch (URISyntaxException e) {
                Logger.getLogger(BDII.class.getName()).log(Level.FINE, "Error creating URI for a storge element.", e);
            }
        }



        /*	searchPhrase = "(&(objectClass=GlueSEAccessProtocol)(|";

        for(String id : ids.keySet()) {
        searchPhrase += "(GlueChunkKey=GlueSEUniqueID=" + id + ")";
        }
        searchPhrase += "))";
        res = q.query(searchPhrase);*/

        /*	for (SearchResult r : res) {
        Attributes attributes = r.getAttributes();
        //System.out.println(r.toString());
        //final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss"; //$NON-NLS-1$
        // Calendar cal = Calendar.getInstance();
        //SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);

        String id = GlueUtility.getStringAttribute( "GlueChunkKey", attributes ); //$NON-NLS-1$
        id = id.substring( id.indexOf( '=' ) + 1 );

        //String Endpoint = GlueUtility.getStringAttribute( "GlueSEAccessProtocolEndpoint", attributes ); //$NON-NLS-1$
        Long port = GlueUtility.getLongAttribute( "GlueSEAccessProtocolPort", attributes ); //$NON-NLS-1$
        String type = GlueUtility.getStringAttribute( "GlueSEAccessProtocolType", attributes ); //$NON-NLS-1$
        //  String Version = GlueUtility.getStringAttribute( "GlueSEAccessProtocolVersion", attributes ); //$NON-NLS-1$

        if(!srmIds.contains(id)) {

        StringBuilder url = new StringBuilder();

        url.append(type);
        url.append("://");
        url.append(id);
        if(port != -1) {
        url.append(':');
        url.append(port);
        }
        url.append(ids.get(id));

        url.append('/');

        URI uri = URI.create(url.toString());

        if(uri.getScheme().equals("gsiftp")) {
        srmURIs.add(uri);
        }
        }

        }*/

        /*for(URI uri: srmURIs) {
        System.out.println("bdii " + uri.toString());
        }*/

        return srmURIs;
    }

    public List<URI> queryWMSURIs(String vo, int timeOut) throws InternalProcessingError {
        BDIIQuery q = new BDIIQuery(location.toString());

        String searchPhrase =
                "(&(objectClass=GlueService)(GlueServiceUniqueID=*)(GlueServiceAccessControlRule=" + vo + ")";
        searchPhrase += "(|"; //$NON-NLS-1$
        for (String type : wmsServiceType) {
            searchPhrase += "(GlueServiceType=" + type + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        searchPhrase += "))";

        ArrayList<SearchResult> res = q.query(searchPhrase, timeOut);

        List<URI> wmsURIs = new LinkedList<URI>();

        for (SearchResult r : res) {

            try {
                URI wmsURI = new URI(r.getAttributes().get("GlueServiceEndpoint").get().toString());
                wmsURIs.add(wmsURI);
            } catch (NamingException ex) {
                Logger.getLogger(BDII.class.getName()).log(Level.WARNING, "Error creating URI for WMS.", ex);
            } catch (URISyntaxException e) {
                Logger.getLogger(BDII.class.getName()).log(Level.WARNING, "Error creating URI for WMS.", e);
            }
        }

        return wmsURIs;
    }
}
