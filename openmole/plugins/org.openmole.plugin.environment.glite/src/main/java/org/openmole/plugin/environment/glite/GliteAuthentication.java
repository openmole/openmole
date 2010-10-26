/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.plugin.environment.glite;

import fr.in2p3.jsaga.adaptor.base.usage.UDuration;
import fr.in2p3.jsaga.adaptor.security.VOMSContext;
import java.io.BufferedInputStream;
import java.io.File;
import org.openmole.commons.tools.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.ogf.saga.context.Context;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.openmole.commons.aspect.caching.Cachable;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.io.FileUtil;
import org.openmole.core.file.URIFile;
import org.openmole.core.model.execution.batch.IBatchServiceAuthentication;
import org.openmole.core.model.file.IURIFile;
import org.openmole.misc.executorservice.ExecutorType;
import org.openmole.plugin.environment.glite.internal.Activator;
import org.openmole.plugin.environment.glite.internal.ProxyChecker;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class GliteAuthentication implements IBatchServiceAuthentication {

    final private String voName;
    final private String vomsURL;
    final private String myProxy;
    final private String myProxyUserID;
    final private String myProxyPass;
    
    transient private File proxy = null;
    transient volatile private long proxyExpiresTime = Long.MAX_VALUE;

    public GliteAuthentication(String voName, String vomsURL, String myProxy, String myProxyUserID, String myProxyPass) {
        this.voName = voName;
        this.vomsURL = vomsURL;
        this.myProxy = myProxy;
        this.myProxyUserID = myProxyUserID;
        this.myProxyPass = myProxyPass;
    }

    @Cachable
    private File getCACertificatesDir() throws InternalProcessingError, InterruptedException {
        try {
            String X509_CERT_DIR = System.getenv("X509_CERT_DIR");

            File caDir;
            if (X509_CERT_DIR != null && (caDir = new File(X509_CERT_DIR)).exists()) {
                return caDir;
            } else {
                caDir = new File("/etc/grid-security/certificates/");
                if (caDir.exists()) {
                    return caDir;
                } else {
                    File tmp = Activator.getWorkspace().getFile("CACertificates");

                    if (!tmp.exists() || !new File(tmp, ".complete").exists()) {
                        tmp.mkdir();
                        dowloadCACertificates(new URI(Activator.getWorkspace().getPreference(GliteEnvironment.CACertificatesSiteLocation)), tmp);
                        new File(tmp, ".complete").createNewFile();
                    }
                    return tmp;
                }
            }

        } catch (URISyntaxException e) {
            throw new InternalProcessingError(e);
        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }

    }

    @Override
    public void initialize() throws UserBadDataError, InternalProcessingError, InterruptedException {
        File proxyFile;

        if (System.getenv().containsKey("X509_USER_PROXY") && (proxyFile = new File(System.getenv().get("X509_USER_PROXY"))).exists()) {
            createContextFromFile(proxyFile);
        } else {
            createContextFromPreferences();
        }
    }

    private void createContextFromPreferences() throws InternalProcessingError, UserBadDataError, InterruptedException {
        Context ctx = Activator.getJSagaSessionService().createContext();
        long proxyDuration = initContext(ctx);

        long interval = (long) (proxyDuration * Activator.getWorkspace().getPreferenceAsDouble(GliteEnvironment.ProxyRenewalRatio));

        Logger.getLogger(GliteAuthentication.class.getName()).log(Level.FINE, "Proxy renewal in {0} ms.", interval);
        Activator.getUpdater().delay(new ProxyChecker(this, ctx), ExecutorType.OWN, interval);

        Activator.getJSagaSessionService().addContext(ctx);
    }

    public long initContext(Context ctx) throws InternalProcessingError, InterruptedException, UserBadDataError {

        Logger.getLogger(GliteAuthentication.class.getName()).log(Level.FINE, "Proxy renewal.");

        
        long proxyDuration;
        
        try {

            ctx.setAttribute(VOMSContext.VOMSDIR, "");
            ctx.setAttribute(Context.CERTREPOSITORY, getCACertificatesDir().getCanonicalPath());

            if (!myProxy.isEmpty()) {
                ctx.setAttribute(Context.TYPE, "VOMSMyProxy");
                Logger.getLogger(GliteAuthentication.class.getName()).log(Level.INFO, myProxy);
                ctx.setAttribute(VOMSContext.MYPROXYSERVER, myProxy);
                ctx.setAttribute(VOMSContext.DELEGATIONLIFETIME, getDelegationTimeString());
                ctx.setAttribute(VOMSContext.MYPROXYUSERID, myProxyUserID);
                ctx.setAttribute(VOMSContext.MYPROXYPASS, myProxyPass);
                proxyDuration = 12 * 60 * 60 * 1000L;
                proxyExpiresTime = Long.MAX_VALUE;
            } else {
                ctx.setAttribute(Context.TYPE, "VOMS");
                ctx.setAttribute(Context.LIFETIME, getTimeString());
                proxyDuration = getTime();
                proxyExpiresTime = System.currentTimeMillis() + getTime();
            }

            if (proxy == null) {
                proxy = Activator.getWorkspace().newFile("proxy", ".x509");

                if (getCertType().equalsIgnoreCase("p12")) {
                    ctx.setAttribute(VOMSContext.USERCERTKEY, getP12CertPath());
                } else if (getCertType().equalsIgnoreCase("pem")) {
                    ctx.setAttribute(Context.USERCERT, getCertPath());
                    ctx.setAttribute(Context.USERKEY, getKeyPath());
                } else {
                    throw new UserBadDataError("Unknown certificate type " + getCertType());
                }

                String keyPassword = Activator.getWorkspace().getPreference(GliteEnvironment.PasswordLocation);
                if (keyPassword == null) {
                    keyPassword = "";
                }
                ctx.setAttribute(Context.USERPASS, keyPassword);

                String fqan = getFQAN();

                if (!fqan.isEmpty()) {
                    ctx.setAttribute(VOMSContext.USERFQAN, fqan);
                }
            }

            ctx.setAttribute(Context.USERPROXY, proxy.getAbsolutePath());
            ctx.setAttribute(Context.SERVER, vomsURL);
            ctx.setAttribute(Context.USERVO, voName);

            ctx.getAttribute(Context.USERID);
 

            return proxyDuration;
            
        } catch (NoSuccessException e) {
            throw new InternalProcessingError(e);
        } catch (NotImplementedException e) {
            throw new InternalProcessingError(e);
        } catch (AuthenticationFailedException e) {
            throw new InternalProcessingError(e);
        } catch (AuthorizationFailedException e) {
            throw new InternalProcessingError(e);
        } catch (PermissionDeniedException e) {
            throw new InternalProcessingError(e);
        } catch (IncorrectStateException e) {
            throw new InternalProcessingError(e);
        } catch (BadParameterException e) {
            throw new InternalProcessingError(e);
        } catch (DoesNotExistException e) {
            throw new InternalProcessingError(e);
        } catch (org.ogf.saga.error.TimeoutException e) {
            throw new InternalProcessingError(e);
        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }
    }

    private static String getCertType() throws InternalProcessingError {
        return Activator.getWorkspace().getPreference(GliteEnvironment.CertificateType);
    }

    private static String getP12CertPath() throws InternalProcessingError {
        return Activator.getWorkspace().getPreference(GliteEnvironment.P12CertificateLocation);
    }

    private static String getCertPath() throws InternalProcessingError {
        return Activator.getWorkspace().getPreference(GliteEnvironment.CertificatePathLocation);
    }

    private static String getKeyPath() throws InternalProcessingError {
        return Activator.getWorkspace().getPreference(GliteEnvironment.KeyPathLocation);
    }

    private static String getFQAN() throws InternalProcessingError {
        return Activator.getWorkspace().getPreference(GliteEnvironment.FqanLocation);
    }

    private static String getTimeString() throws InternalProcessingError {
        return Activator.getWorkspace().getPreference(GliteEnvironment.TimeLocation);
    }

    private static long getTime() throws InternalProcessingError, UserBadDataError {
        try {
            return UDuration.toInt(getTimeString()) * 1000L;
        } catch (ParseException ex) {
            throw new UserBadDataError(ex);
        }
    }

    private static String getDelegationTimeString() throws InternalProcessingError {
        return Activator.getWorkspace().getPreference(GliteEnvironment.DelegationTimeLocation);
    }

    static void dowloadCACertificates(URI uri, File dir) throws InternalProcessingError, IOException, InterruptedException {

        IURIFile site = new URIFile(uri);

        for (String tarUrl : site.list()) {
            try {
                IURIFile child = site.getChild(tarUrl);
                InputStream is = child.openInputStream();

                TarArchiveInputStream tis = new TarArchiveInputStream(new GZIPInputStream(new BufferedInputStream(is)));

                try {
                    //Bypass the directory
                    TarArchiveEntry tarEntry = tis.getNextTarEntry();

                    tarEntry = tis.getNextTarEntry();

                    while (tarEntry != null) {
                        File dest = new File(dir, tarEntry.getName());
                        dest = new File(dir, dest.getName());

                        if (dest.exists()) {
                            dest.delete();
                        }

                        FileOutputStream os = new FileOutputStream(dest);
                        try {
                            FileUtil.copy(tis, os);
                        } finally {
                            os.close();
                        }

                        tarEntry = tis.getNextTarEntry();
                    }
                } catch (IOException e) {
                    Logger.getLogger(GliteAuthentication.class.getName()).log(Level.WARNING, "Unable to untar " + child.toString(), e);
                } finally {
                    tis.close();
                }
            } catch (IOException e) {
                throw new IOException(tarUrl, e);
            }
        }
    }

    private void createContextFromFile(File proxyFile) throws InternalProcessingError, InterruptedException {
        try {
            Context ctx = Activator.getJSagaSessionService().createContext();
            ctx.setAttribute(Context.USERPROXY, proxyFile.getCanonicalPath());
            ctx.setAttribute(Context.CERTREPOSITORY, getCACertificatesDir().getCanonicalPath());
            ctx.setAttribute(VOMSContext.VOMSDIR, "");
            ctx.setAttribute(Context.TYPE, "GlobusLegacy");

            Activator.getJSagaSessionService().addContext(ctx);
        } catch (NotImplementedException e) {
            throw new InternalProcessingError(e);
        } catch (AuthenticationFailedException e) {
            throw new InternalProcessingError(e);
        } catch (AuthorizationFailedException e) {
            throw new InternalProcessingError(e);
        } catch (PermissionDeniedException e) {
            throw new InternalProcessingError(e);
        } catch (IncorrectStateException e) {
            throw new InternalProcessingError(e);
        } catch (BadParameterException e) {
            throw new InternalProcessingError(e);
        } catch (DoesNotExistException e) {
            throw new InternalProcessingError(e);
        } catch (org.ogf.saga.error.TimeoutException e) {
            throw new InternalProcessingError(e);
        } catch (NoSuccessException e) {
            throw new InternalProcessingError(e);
        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }
    }

    public long getProxyExpiresTime() {
        return proxyExpiresTime;
    }
}
