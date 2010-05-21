
/*
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
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
package org.openmole.core.workflow.methods.libraries;

import java.io.IOException;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.openmole.core.workflow.model.job.IContext;

public class PlatformDakota extends Platform {
    private String wsURL = "http://localhost:8080/dakota_ws/sample";
    // To dump posted datas
    //private String wsURL = "http://cfp6040.clermont.cemagref.fr/~florent.chuffart/ProtoGr/html/print_r.php";
    public String getWsURL() {
        return wsURL;
    }

    public void setWsURL(String wsURL) {
        this.wsURL = wsURL;
    }

    @Override
    public void load(Library library) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getInvokeCode(MethodInstance method) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object invoke(IContext context, MethodInstance mi) {
        HttpClient  client = new HttpClient();
        PostMethod post = new PostMethod(wsURL);
        post.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        // Make a loop to prepare key and value
        ArrayList<Part> parts = new  ArrayList<Part>();//new Part[];// [mi.getParametersValues().length];
        for(int i = 0; i<mi.getParametersValues().length ; i++)
        {
            if (mi.getParametersTypes()[i] != ArrayList.class) {
                parts.add(new StringPart(mi.getParametersNames()[i],mi.getParametersValues()[i].toString()));
            } else {
                ArrayList al = (ArrayList) mi.getParametersValues()[i] ;
                for (int j = 0; j < al.size() ; j++) {
                  HashMap h = (HashMap) al.get(j);
                  for ( Object e : h.entrySet()){
                    Map.Entry<String, String> me = ( Map.Entry<String, String>) e ;
                    parts.add(new StringPart("factors[" + (j+1) + "][" + me.getKey() + "]", me.getValue()));
                  }
               }
            }
        }
        //for (int k = 0; k < parts.size() ; k++) {
          //System.out.println("***" +parts.get(k) );
        //}
        Part[] a = new Part[parts.size()];
        parts.toArray(a);
        post.setRequestEntity(new MultipartRequestEntity(a, post.getParams()));
        Object ro = null;
        try {
            client.executeMethod(post);
            ro = post.getResponseBodyAsString();
        } catch (IOException ex) {
        	Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex);
        }
        return ro ;
    }

    @Override
    public void close(Library library) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void install() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}