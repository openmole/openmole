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
package org.openmole.plugin.environmentprovider.glite;

import org.openmole.plugin.environmentprovider.glite.internal.OverSubmissionAgent;
import org.openmole.plugin.environmentprovider.glite.internal.ProxyChecker;
import org.openmole.plugin.environmentprovider.glite.internal.DicotomicWorkloadStrategy;
import org.openmole.plugin.environmentprovider.glite.internal.GliteLaunchingScript;
import org.openmole.plugin.environmentprovider.glite.internal.WorkloadOnAverages;
import org.openmole.plugin.environmentprovider.glite.internal.BDII;
import fr.in2p3.jsaga.adaptor.base.usage.UDuration;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.openmole.misc.tools.io.FastCopy;

import org.ogf.saga.context.Context;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.PermissionDeniedException;
import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.core.file.URIFile;
import org.openmole.core.workflow.model.execution.batch.IBatchStorage;
import org.openmole.core.workflow.model.file.IURIFile;
import org.openmole.misc.caching.Cachable;
import org.openmole.misc.workspace.ConfigurationElement;

import fr.in2p3.jsaga.adaptor.security.VOMSContext;
import java.util.Collection;
import java.util.LinkedList;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.openmole.misc.executorservice.ExecutorType;

import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.core.workflow.implementation.execution.batch.BatchStorage;
import org.openmole.misc.workspace.InteractiveConfiguration;
import org.openmole.plugin.environmentprovider.glite.internal.Activator;

import org.openmole.plugin.environmentprovider.jsaga.JSAGAEnvironment;
import org.openmole.plugin.environmentprovider.jsaga.JSAGAJobService;
import org.openmole.plugin.environmentprovider.jsaga.model.IJSAGAJobService;
import org.openmole.plugin.environmentprovider.jsaga.model.IJSAGALaunchingScript;

public class GliteEnvironment extends JSAGAEnvironment<GliteEnvironmentDescription> {

    final static String ConfigGroup = GliteEnvironment.class.getSimpleName();
    @InteractiveConfiguration(label = "Certificate location")
    final static ConfigurationLocation CertificatePathLocation = new ConfigurationLocation(ConfigGroup, "CertificatePath");
    @InteractiveConfiguration(label = "Key location")
    final static ConfigurationLocation KeyPathLocation = new ConfigurationLocation(ConfigGroup, "KeyPath");
    @InteractiveConfiguration(label = "Key password")
    final static ConfigurationLocation PasswordLocation = new ConfigurationLocation(ConfigGroup, "Password", true);
    @InteractiveConfiguration(label = "Fqan")
    final static ConfigurationLocation FqanLocation = new ConfigurationLocation(ConfigGroup, "Fqan");
    final static ConfigurationLocation TimeLocation = new ConfigurationLocation(ConfigGroup, "Time");
    final static ConfigurationLocation FetchRessourcesTimeOutLocation = new ConfigurationLocation(ConfigGroup, "FetchRessourcesTimeOut");
    final static ConfigurationLocation CACertificatesSiteLocation = new ConfigurationLocation(ConfigGroup, "CACertificatesSite");
    final static ConfigurationLocation OverSubmissionIntervalLocation = new ConfigurationLocation(ConfigGroup, "OverSubmissionInterval");
    final static ConfigurationLocation OverSubmissionRatioWaitingLocation = new ConfigurationLocation(ConfigGroup, "OverSubmissionRatioWaiting");
    final static ConfigurationLocation OverSubmissionRatioRunningLocation = new ConfigurationLocation(ConfigGroup, "OverSubmissionRatioRunning");
    final static ConfigurationLocation OverSubmissionMinJob = new ConfigurationLocation(ConfigGroup, "OverSubmissionMinJob");
    final static ConfigurationLocation OverSubmissionNumberOfJobUnderMin = new ConfigurationLocation(ConfigGroup, "OverSubmissionNumberOfJobUnderMin");
    final static ConfigurationLocation OverSubmissionRatioEpsilonLocation = new ConfigurationLocation(ConfigGroup, "OverSubmissionRatioEpsilon");
    final static ConfigurationLocation LocalThreadsBySELocation = new ConfigurationLocation(ConfigGroup, "LocalThreadsBySE");
    final static ConfigurationLocation LocalThreadsByWMSLocation = new ConfigurationLocation(ConfigGroup, "LocalThreadsByWMS");
    final static ConfigurationLocation ProxyRenewalRatio = new ConfigurationLocation(ConfigGroup, "ProxyRenewalRatio");

    // final static Integer RemoteThreadsBySE = 2;
    static {
        Activator.getWorkspace().addToConfigurations(CertificatePathLocation, new ConfigurationElement() {

            @Override
            public String getDefaultValue() {
                return System.getProperty("user.home") + "/.globus/usercert.pem";
            }
        });

        Activator.getWorkspace().addToConfigurations(KeyPathLocation, new ConfigurationElement() {

            @Override
            public String getDefaultValue() {
                return System.getProperty("user.home") + "/.globus/userkey.pem";
            }
        });

        Activator.getWorkspace().addToConfigurations(TimeLocation, "PT24H");
        Activator.getWorkspace().addToConfigurations(FetchRessourcesTimeOutLocation, Integer.toString(2 * 60 * 1000));
        Activator.getWorkspace().addToConfigurations(CACertificatesSiteLocation, "http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/");
        Activator.getWorkspace().addToConfigurations(FqanLocation, "");

        Activator.getWorkspace().addToConfigurations(LocalThreadsBySELocation, "5");
        Activator.getWorkspace().addToConfigurations(LocalThreadsByWMSLocation, "5");


        Activator.getWorkspace().addToConfigurations(ProxyRenewalRatio, "0.2");

        Activator.getWorkspace().addToConfigurations(OverSubmissionRatioWaitingLocation, "0.5");
        Activator.getWorkspace().addToConfigurations(OverSubmissionRatioRunningLocation, "0.2");
        Activator.getWorkspace().addToConfigurations(OverSubmissionRatioEpsilonLocation, "0.01");
        Activator.getWorkspace().addToConfigurations(OverSubmissionIntervalLocation, Integer.toString(5 * 60 * 1000));


        Activator.getWorkspace().addToConfigurations(OverSubmissionMinJob, Integer.toString(100));
        Activator.getWorkspace().addToConfigurations(OverSubmissionNumberOfJobUnderMin, Integer.toString(3));


        //    Activator.getUpdater().delay(new WaitingThreadInterrupter(), ExecutorType.OWN);
    }
    transient WorkloadOnAverages workload;
    transient File CACertificatesDir;
    transient BDII bdii;
    transient ProxyChecker proxyChecker;
    transient Integer threadsByWMS;
    transient Integer threadsBySE;
    transient File proxy;

    GliteEnvironment(GliteEnvironmentDescription description) throws InternalProcessingError {
        super(description);
        threadsBySE = Activator.getWorkspace().getPreferenceAsInt(LocalThreadsBySELocation);
        threadsByWMS = Activator.getWorkspace().getPreferenceAsInt(LocalThreadsByWMSLocation);
        Double overSubmissionWaitingRatio = Activator.getWorkspace().getPreferenceAsDouble(OverSubmissionRatioWaitingLocation);
        Double overSubmissionRunningRatio = Activator.getWorkspace().getPreferenceAsDouble(OverSubmissionRatioRunningLocation);
        Double overSubmissionEpsilonRatio = Activator.getWorkspace().getPreferenceAsDouble(OverSubmissionRatioEpsilonLocation);
        Long overSubmissionInterval = Activator.getWorkspace().getPreferenceAsLong(OverSubmissionIntervalLocation);
        Integer minJobs = Activator.getWorkspace().getPreferenceAsInt(OverSubmissionMinJob);
        Integer numberOfJobUnderMin = Activator.getWorkspace().getPreferenceAsInt(OverSubmissionNumberOfJobUnderMin);
        //Activator.getUpdater().registerForUpdate(new OverSubmissionAgent(this, new WorkloadOnAverages(MinimumStatistic, ResubmitRatioWating, KillRatioWaiting, ResubmitRatioRunning, KillRatioRunning, MaxNumberOfSimultaneousExecutionForAJob, this), Activator.getWorkspace().getPreferenceAsInt(MinimumNumberOfJobsLocation), Activator.getWorkspace().getPreferenceAsInt(NumberOfSimultaneousExecutionForAJobWhenUnderMinJobLocation)), ExecutorType.OWN);
        Activator.getUpdater().registerForUpdate(new OverSubmissionAgent(this, new DicotomicWorkloadStrategy(overSubmissionWaitingRatio, overSubmissionRunningRatio, overSubmissionEpsilonRatio), minJobs, numberOfJobUnderMin, overSubmissionInterval), ExecutorType.OWN);

        // Activator.getUpdater().registerForUpdate(new WaitingThreadInterrupter(), ExecutorType.OWN);

    }

    private File getCACertificatesDir() throws InternalProcessingError, InterruptedException {
        if (CACertificatesDir != null) {
            return CACertificatesDir;
        }

        synchronized (this) {
            if (CACertificatesDir == null) {

                try {
                    if (CACertificatesDir == null) {
                        String X509_CERT_DIR = System.getenv("X509_CERT_DIR");

                        File caDir;
                        if (X509_CERT_DIR != null && (caDir = new File(X509_CERT_DIR)).exists()) {
                            CACertificatesDir = caDir;
                        } else {
                            caDir = new File("/etc/grid-security/certificates/");
                            if (caDir.exists()) {
                                CACertificatesDir = caDir;
                            } else {
                                File tmp = Activator.getWorkspace().getFile("CACertificates");

                                if (!tmp.exists() || !new File(tmp, ".complete").exists()) {
                                    tmp.mkdir();
                                    dowloadCACertificates(new URI(Activator.getWorkspace().getPreference(CACertificatesSiteLocation)), tmp);
                                    new File(tmp, ".complete").createNewFile();
                                }
                                CACertificatesDir = tmp;
                            }
                        }
                    }
                } catch (URISyntaxException e) {
                    throw new InternalProcessingError(e);
                } catch (IOException e) {
                    throw new InternalProcessingError(e);
                }

            }
        }

        return CACertificatesDir;
    }

    @Override
    public void initializeAccess() throws UserBadDataError, InternalProcessingError, InterruptedException {
        File proxyFile;

        if (System.getenv().containsKey("X509_USER_PROXY") && (proxyFile = new File(System.getenv().get("X509_USER_PROXY"))).exists()) {
            createContextFromFile(proxyFile);
        } else {
            createContextFromPreferences();
        }
    }

    private void createContextFromPreferences() throws InternalProcessingError, UserBadDataError, InterruptedException {


        Context ctx = Activator.getJSagaSessionService().createContext();
        initContext(ctx);
        String time = getTime();
        long interval;
        try {
            interval = ((long) (UDuration.toInt(time) * Activator.getWorkspace().getPreferenceAsDouble(ProxyRenewalRatio))) * 1000;
            //  Logger.getLogger(GliteEnvironment.class.getName()).log(Level.INFO, "Proxy renewal interval: " + interval);
        } catch (ParseException ex) {
            throw new UserBadDataError(ex);
        }
        if (proxyChecker == null) {
            proxyChecker = new ProxyChecker(this, ctx, interval);
            Activator.getUpdater().delay(proxyChecker, ExecutorType.OWN);

        } else {
            proxyChecker.setCtx(ctx);
            proxyChecker.setInterval(interval);
        }


        //	ctx.getAttribute(Context.LIFETIME);
        Activator.getJSagaSessionService().addContext(ctx);

    }

    public void initContext(Context ctx) throws InternalProcessingError, InterruptedException {

        // Logger.getLogger(GliteEnvironment.class.getName()).log(Level.INFO, "Initializing context");

        try {
            //synchronized (ctx) {
            ctx.setAttribute(Context.TYPE, "VOMS");
            ctx.setAttribute(VOMSContext.VOMSDIR, "");
            ctx.setAttribute(Context.CERTREPOSITORY, getCACertificatesDir().getCanonicalPath());

            ctx.setAttribute(Context.LIFETIME, getTime());

            // String proxyPath = ctx.getAttribute(Context.USERPROXY);
            // if(proxyPath == null)

            if (proxy == null) {
                proxy = Activator.getWorkspace().newTmpFile("proxy", ".x509");
            }
            ctx.setAttribute(Context.USERPROXY, proxy.getCanonicalPath());

            ctx.setAttribute(Context.USERCERT, getCertPath());
            ctx.setAttribute(Context.USERKEY, getKeyPath());

            ctx.setAttribute(Context.SERVER, getVomsURL());
            ctx.setAttribute(Context.USERVO, getVoName());

            String fqan = getFQAN();

            if (!fqan.isEmpty()) {
                ctx.setAttribute(VOMSContext.USERFQAN, fqan);
            }

            String keyPassword = Activator.getWorkspace().getPreference(PasswordLocation);

            ctx.setAttribute(Context.USERPASS, keyPassword);

            //FIXME For testing puroposes
            //ctx.getAttribute(Context.USERID);

            //  }
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

    private static String getCertPath() throws InternalProcessingError {
        return Activator.getWorkspace().getPreference(CertificatePathLocation);
    }

    private static String getKeyPath() throws InternalProcessingError {
        return Activator.getWorkspace().getPreference(KeyPathLocation);
    }

    private static String getFQAN() throws InternalProcessingError {
        return Activator.getWorkspace().getPreference(FqanLocation);
    }

    private static String getTime() throws InternalProcessingError {
        return Activator.getWorkspace().getPreference(TimeLocation);
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
                            FastCopy.copy(tis, os);

                        } finally {
                            os.close();
                        }

                        tarEntry = tis.getNextTarEntry();
                    }
                } catch (IOException e) {
                    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Unable to untar " + child.toString(), e);
                } finally {
                    tis.close();
                }
            } catch (IOException e) {
                throw new IOException(tarUrl, e);
            }
        }
    }

//   @Cachable
    @Override
    public Collection<IJSAGAJobService> allJobServices() throws InternalProcessingError, UserBadDataError {

        List<URI> jss = getBDII().queryWMSURIs(getVoName(), Activator.getWorkspace().getPreferenceAsInt(FetchRessourcesTimeOutLocation));

        Collection<IJSAGAJobService> jobServices = new LinkedList<IJSAGAJobService>();

        for (URI js : jss) {
            try {
                URI wms = new URI("wms:" + js.getRawSchemeSpecificPart());

                IJSAGAJobService jobService = new JSAGAJobService(wms, this, threadsByWMS);
                jobServices.add(jobService);
            } catch (URISyntaxException e) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "wms:" + js.getRawSchemeSpecificPart(), e);
            }
        }

        return jobServices;
    }

//    @Cachable
    @Override
    public Collection<IBatchStorage> allStorages() throws InternalProcessingError, UserBadDataError {

        Collection<IBatchStorage> allStorages = new LinkedList<IBatchStorage>();

        Set<URI> stors = getBDII().querySRMURIs(getVoName(), Activator.getWorkspace().getPreferenceAsInt(FetchRessourcesTimeOutLocation));

        for (URI stor : stors) {
            IBatchStorage storage = new BatchStorage(stor, this, threadsBySE);
            allStorages.add(storage);
        }

        return allStorages;

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

    @Cachable
    @Override
    public IJSAGALaunchingScript<GliteEnvironment> getLaunchingScript() {
        return new GliteLaunchingScript(getMemorySizeForRuntime());
    }

    public String getVoName() {
        return getDescription().getVoName();
    }

    String getBDIIURI() {
        return getDescription().getBDII();
    }

    String getVomsURL() {
        return getDescription().getVomsURL();
    }

    private BDII getBDII() {
        if (bdii != null) {
            return bdii;
        }

        synchronized (this) {
            if (bdii == null) {
                bdii = new BDII(getBDIIURI());
            }

            return bdii;
        }

    }

    /*  @Override
    public void setConfigurationMode(EnvironmentConfiguration configuration) throws InternalProcessingError {
    switch (configuration) {
    case Local:
    threadsBySE = Activator.getWorkspace().getPreferenceAsInt(LocalThreadsBySELocation);
    break;
    case Remote:
    threadsBySE = RemoteThreadsBySE;
    break;
    }
    }*/
}
