/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

import java.net.URI;
import java.net.URISyntaxException;
import org.openmole.plugin.environment.glite.internal.OverSubmissionAgent;
import org.openmole.plugin.environment.glite.internal.DicotomicWorkloadStrategy;
import org.openmole.plugin.environment.glite.internal.GliteLaunchingScript;
import org.openmole.plugin.environment.glite.internal.WorkloadOnAverages;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.aspect.caching.Cachable;
import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.execution.batch.BatchStorage;
import org.openmole.core.model.execution.batch.IBatchStorage;
import org.openmole.misc.workspace.ConfigurationElement;

import org.openmole.misc.executorservice.ExecutorType;

import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.misc.workspace.InteractiveConfiguration;
import org.openmole.plugin.environment.glite.internal.Activator;
import org.openmole.plugin.environment.glite.internal.BDII;

import org.openmole.plugin.environment.jsaga.JSAGAEnvironment;
import org.openmole.plugin.environment.jsaga.IJSAGALaunchingScript;
import org.openmole.plugin.environment.jsaga.JSAGAJobService;

public class GliteEnvironment extends JSAGAEnvironment {

    final static String ConfigGroup = GliteEnvironment.class.getSimpleName();

    @InteractiveConfiguration(label = "CertificateType type", choices = {"pem", "p12"})
    final static ConfigurationLocation CertificateType = new ConfigurationLocation(ConfigGroup, "CertificateType");

    @InteractiveConfiguration(label = "PEM Certificate location", dependOn = "CertificateType", value = "pem")
    final static ConfigurationLocation CertificatePathLocation = new ConfigurationLocation(ConfigGroup, "CertificatePath");

    @InteractiveConfiguration(label = "PEM Key location", dependOn = "CertificateType", value = "pem")
    final static ConfigurationLocation KeyPathLocation = new ConfigurationLocation(ConfigGroup, "KeyPath");

    @InteractiveConfiguration(label = "P12 Certificate Location", dependOn = "CertificateType", value = "p12")
    final static ConfigurationLocation P12CertificateLocation = new ConfigurationLocation(ConfigGroup, "P12CertificateLocation");

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

        Activator.getWorkspace().addToConfigurations(P12CertificateLocation, new ConfigurationElement() {

            @Override
            public String getDefaultValue() {
                return System.getProperty("user.home") + "/.globus/certificate.p12";
            }
        });

        Activator.getWorkspace().addToConfigurations(CertificateType, "pem");
        Activator.getWorkspace().addToConfigurations(TimeLocation, "PT24H");
        Activator.getWorkspace().addToConfigurations(FetchRessourcesTimeOutLocation, "PT2M");
        Activator.getWorkspace().addToConfigurations(CACertificatesSiteLocation, "http://dist.eugridpma.info/distribution/igtf/current/accredited/tgz/");
        Activator.getWorkspace().addToConfigurations(FqanLocation, "");

        Activator.getWorkspace().addToConfigurations(LocalThreadsBySELocation, "10");
        Activator.getWorkspace().addToConfigurations(LocalThreadsByWMSLocation, "10");

        Activator.getWorkspace().addToConfigurations(ProxyRenewalRatio, "0.2");

        Activator.getWorkspace().addToConfigurations(OverSubmissionRatioWaitingLocation, "0.5");
        Activator.getWorkspace().addToConfigurations(OverSubmissionRatioRunningLocation, "0.2");
        Activator.getWorkspace().addToConfigurations(OverSubmissionRatioEpsilonLocation, "0.01");
        Activator.getWorkspace().addToConfigurations(OverSubmissionIntervalLocation, "PT5M");

        Activator.getWorkspace().addToConfigurations(OverSubmissionMinJob, Integer.toString(100));
        Activator.getWorkspace().addToConfigurations(OverSubmissionNumberOfJobUnderMin, Integer.toString(3));
    }

    WorkloadOnAverages workload;
    Integer threadsByWMS;
    Integer threadsBySE;
    
    final String voName;
    final String vomsURL;
    final String bdiiURL;
    
    public GliteEnvironment(String voName, String vomsURL, String bdii) throws InternalProcessingError {
        super(Collections.EMPTY_MAP);
        this.bdiiURL = bdii;
        this.voName = voName;
        this.vomsURL = vomsURL;
        init();
    }

    public GliteEnvironment(String voName, String vomsURL, String bdii, Map<String, String> attributes) throws InternalProcessingError {
        super(attributes);
        this.bdiiURL = bdii;
        this.voName = voName;
        this.vomsURL = vomsURL;
        init();
    }

    
    public GliteEnvironment(String voName, String vomsURL, String bdii, int memoryForRuntime, Map<String, String> attributes) throws InternalProcessingError {
        super(memoryForRuntime, attributes);
        this.bdiiURL = bdii;
        this.voName = voName;
        this.vomsURL = vomsURL;
        init();
    }

    private void init() throws InternalProcessingError{
        threadsBySE = Activator.getWorkspace().getPreferenceAsInt(LocalThreadsBySELocation);
        threadsByWMS = Activator.getWorkspace().getPreferenceAsInt(LocalThreadsByWMSLocation);
        Double overSubmissionWaitingRatio = Activator.getWorkspace().getPreferenceAsDouble(OverSubmissionRatioWaitingLocation);
        Double overSubmissionRunningRatio = Activator.getWorkspace().getPreferenceAsDouble(OverSubmissionRatioRunningLocation);
        Double overSubmissionEpsilonRatio = Activator.getWorkspace().getPreferenceAsDouble(OverSubmissionRatioEpsilonLocation);
        long overSubmissionInterval = Activator.getWorkspace().getPreferenceAsDurationInMs(OverSubmissionIntervalLocation);
        Integer minJobs = Activator.getWorkspace().getPreferenceAsInt(OverSubmissionMinJob);
        Integer numberOfJobUnderMin = Activator.getWorkspace().getPreferenceAsInt(OverSubmissionNumberOfJobUnderMin);
        Activator.getUpdater().registerForUpdate(new OverSubmissionAgent(this, new DicotomicWorkloadStrategy(overSubmissionWaitingRatio, overSubmissionRunningRatio, overSubmissionEpsilonRatio), minJobs, numberOfJobUnderMin), ExecutorType.OWN, overSubmissionInterval);
    }


    @Cachable
    @Override
    public IJSAGALaunchingScript getLaunchingScript() {
        return new GliteLaunchingScript(this);
    }

    public String getVOName() {
        return voName;
    }

    @Override
    public Collection<JSAGAJobService> allJobServices() throws InternalProcessingError, UserBadDataError, InterruptedException {

        List<URI> jss = getBDII().queryWMSURIs(getVOName(), new Long(Activator.getWorkspace().getPreferenceAsDurationInMs(FetchRessourcesTimeOutLocation)).intValue());

        Collection<JSAGAJobService> jobServices = new LinkedList<JSAGAJobService>();

        for (URI js : jss) {
            try {
                URI wms = new URI("wms:" + js.getRawSchemeSpecificPart());

                JSAGAJobService jobService = new GliteJobService(wms, this, new GliteAuthenticationKey(voName, vomsURL), new GliteAuthentication(voName, vomsURL), threadsByWMS);
                jobServices.add(jobService);
            } catch (URISyntaxException e) {
                Logger.getLogger(GliteEnvironment.class.getName()).log(Level.WARNING, "wms:" + js.getRawSchemeSpecificPart(), e);
            }
        }
        return jobServices;
    }

    @Override
    public Collection<IBatchStorage> allStorages() throws InternalProcessingError, UserBadDataError, InterruptedException {

        Collection<IBatchStorage> allStorages = new LinkedList<IBatchStorage>();

        Set<URI> stors = getBDII().querySRMURIs(getVOName(), new Long(Activator.getWorkspace().getPreferenceAsDurationInMs(GliteEnvironment.FetchRessourcesTimeOutLocation)).intValue());

        for (URI stor : stors) {
            IBatchStorage storage = new BatchStorage(stor, this, new GliteAuthenticationKey(voName, vomsURL), new GliteAuthentication(voName, vomsURL),threadsBySE);
            allStorages.add(storage);
        }

        return allStorages;

    }

    @SoftCachable
    private BDII getBDII() {
        return new BDII(bdiiURL);
    }
    
}
