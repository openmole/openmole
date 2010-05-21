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

package org.openmole.core.workflow.methods.resource;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openmole.misc.exception.InternalProcessingError;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.core.workflow.implementation.resource.TarResource;
import org.openmole.core.workflow.implementation.tools.ReplacementSet;
import org.openmole.core.workflow.implementation.tools.VariableExpansion;
import org.openmole.core.workflow.model.execution.IEnvironment;
import org.openmole.core.workflow.model.execution.IEnvironmentDescription;
import org.openmole.core.workflow.model.file.IURIFile;
import org.openmole.core.workflow.model.resource.ILocalFileCache;
import org.openmole.core.workflow.model.resource.IResource;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.misc.caching.Cachable;

@Deprecated
public class OctaveRessource {

	//TarResource archive = new TarResource();

	transient Boolean useNative;
	transient String version;
	transient Boolean deployed;

	String [] repInOctavePath = {
			"libexec/octave/${version}/site/oct/i686-pc-linux-gnu" ,
			"libexec/octave/site/oct/api-v32/i686-pc-linux-gnu" ,
			"libexec/octave/site/oct/i686-pc-linux-gnu" ,
			"share/octave/${version}/site/m" ,
			"share/octave/site/api-v32/m" ,
			"share/octave/site/m" ,
			"share/octave/site/m/startup" ,
			"libexec/octave/${version}/oct/i686-pc-linux-gnu" ,
			"share/octave/${version}/m" ,
			"share/octave/${version}/m/audio" ,
			"share/octave/${version}/m/control" ,
			"share/octave/${version}/m/control/base" ,
			"share/octave/${version}/m/control/hinf" ,
			"share/octave/${version}/m/control/obsolete" ,
			"share/octave/${version}/m/control/system",
			"share/octave/${version}/m/control/util" ,
			"share/octave/${version}/m/deprecated" ,
			"share/octave/${version}/m/elfun" ,
			"share/octave/${version}/m/finance" ,
			"share/octave/${version}/m/general" ,
			"share/octave/${version}/m/geometry" ,
			"share/octave/${version}/m/image" ,
			"share/octave/${version}/m/io" ,
			"share/octave/${version}/m/linear-algebra" ,
			"share/octave/${version}/m/miscellaneous" ,
			"share/octave/${version}/m/optimization" ,
			"share/octave/${version}/m/path" ,
			"share/octave/${version}/m/pkg" ,
			"share/octave/${version}/m/plot" ,
			"share/octave/${version}/m/polynomial" ,
			"share/octave/${version}/m/quaternion" ,
			"share/octave/${version}/m/set" ,
			"share/octave/${version}/m/signal" ,
			"share/octave/${version}/m/sparse" ,
			"share/octave/${version}/m/specfun" ,
			"share/octave/${version}/m/special-matrix" ,
			"share/octave/${version}/m/startup" ,
			"share/octave/${version}/m/statistics" ,
			"share/octave/${version}/m/statistics/base" ,
			"share/octave/${version}/m/statistics/distributions" ,
			"share/octave/${version}/m/statistics/models" ,
			"share/octave/${version}/m/statistics/tests" ,
			"share/octave/${version}/m/strings" ,
			"share/octave/${version}/m/testfun",
			"share/octave/${version}/m/time"
	};

	transient String octavePathCache;
/*
	@Override
	public synchronized void deploy(IEnvironment forEnv, ILocalFileCache fileCache) throws InternalProcessingError, UserBadDataError {

		if(!hasBeenDeployed()) {
			try {
				String versionStr = initVersion("octave", new Shell());
				if(versionStr != null) {	
					setVersion(versionStr);

					if(getVersionNumber() >= 3.0) {
						setUseNative(true);
						setDeployed(true);
						return;
					}
				}
			} catch (IllegalStateException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING,"Could not test if octave is installed.",e);
			} catch (IOException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING,"Could not test if octave is installed.",e);
			}

			archive.deploy(forEnv, fileCache);
			try {
				Shell sh = new Shell();
				initShell(sh);
				setVersion(initVersion(getOctaveBinPath().getCanonicalPath(), sh));
				if(getVersion() == null) {
					throw new InternalProcessingError("Could not identify the version of octave.");
				} else if(getVersionNumber() < 3.0) {
					throw new InternalProcessingError("incorect version of octave (> 3.0) " + getVersionNumber());
				}
			} catch (IllegalStateException e) {
				throw new InternalProcessingError(e);
			} catch (IOException e) {
				throw new InternalProcessingError(e);
			}

			setDeployed(true);
		}
	}


	String initVersion(String octavePath, Shell sh) throws IllegalStateException, IOException {
		//	Shell sh = new Shell();
		//	System.out.println(octavePath);

		ProcessConsumer p = sh.command(octavePath + " --version");
		p.error(new OutputStream(){
			@Override
			public void write(int b) throws IOException {}		
		});

		String ret = p.consumeAsString();
		//System.out.println("string : " + ret);

		if(!ret.isEmpty()) {
			Scanner scan = new Scanner(ret);

			String versionStr = scan.findInLine("[0-9]\\.[0-9]\\.[0-9]");
			return versionStr;
		} else return null;

	}


	void setVersion(String version) {
		this.version = version;
	}

	String getVersion() {
		return version;
	}

	Double getVersionNumber() {
		return Double.valueOf(getVersion().substring(0, getVersion().lastIndexOf('.')));
	}

	public void setArchiveForEnvironment(IEnvironmentDescription<?> env, File file) {
		archive.setArchiveForEnvironment(env, file);
	}



	public void setArchiveForEnvironment(IEnvironmentDescription<?> envDesc, String file)
	throws InternalProcessingError, UserBadDataError {
		archive.setArchiveForEnvironment(envDesc, file);
	}


	public File getDeployDir() {
		return archive.getDeployDir();
	}

	public void execute(File inDir, String src) throws InternalProcessingError{
		try {
			Shell shell = new Shell();
			shell.setDirectory(inDir);

			ProcessConsumer c;
			if(getUseNative()) {
				c = shell.command( "octave " + " --eval \"try " + src + " catch fprintf(stderr(),'%s\\n',lasterror.message) , exit end_try_catch" + '"' );
			}else {
				initShell(shell);
				c = shell.command(getOctaveBinPath().getCanonicalPath() + " --norc  --path .:" + getOctavePath() + " --eval \"try " + src + " catch fprintf(stderr(),'%s\\n',lasterror.message) , exit end_try_catch" + '"' );
			}
			c.consume();
		} catch (IOException e) {
			throw new InternalProcessingError(e);
		}

	}


	private void initShell(Shell sh) throws InternalProcessingError {
		//TODO and on non posix env ???
		try {
			List<File> dirs =  getOctaveLib();
			StringBuilder builder = new StringBuilder();

			boolean fisrt = true;

			for(File d: dirs) {
				if(!fisrt) builder.append(':');
				builder.append(d.getCanonicalPath());
				fisrt = false;
			}

			sh.getUserEnv().put("LD_LIBRARY_PATH", builder.toString());
		} catch (IOException e) {
			throw new InternalProcessingError(e);
		}
		//sh.getUserEnv().put("PATH", getOctavePath().getParent() + ":$PATH");
	}


	private List<File> getAllChildsDir(File dir) {	
		File[] child = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return new File(dir,name).isDirectory();
			}
		});

		List<File> allChilds = new LinkedList<File>();
		allChilds.add(dir);

		for(File f: child) {
			allChilds.addAll(getAllChildsDir(f));
		}

		return allChilds;
	}


	private List<File> getOctaveLib() {
		File ret = new File(getOctaveBasePath(), "lib");



		//System.out.println(ret.getAbsolutePath() + " " + ret.exists());
		//	ret.setExecutable(true);
		//	System.out.println(ret.canExecute());
		return getAllChildsDir(ret);
	}

	private File getOctaveBasePath() {
		File ret = getDeployDir();
		return ret; 
	}


	private  String getOctavePath () throws InternalProcessingError {
		try {
			if(octavePathCache != null) return octavePathCache;

			synchronized (this) {

				if(octavePathCache == null) {
					ReplacementSet replace = new ReplacementSet();
					replace.put("version", getVersion());

					StringBuilder build = new StringBuilder();

					for(String s : repInOctavePath) {
						String literal = VariableExpansion.getInstance().expandData(replace, s);

						build.append(':');
						build.append(new File(getOctaveBasePath(), literal).getCanonicalPath());
					}

					octavePathCache = build.toString();
				}
				return octavePathCache;
			}
		}
		catch (IOException e) {
			throw new InternalProcessingError(e);
		}
	}

	@Cachable
	private File getOctaveBinPath() {
		File octaveBinCache = new File(getOctaveBasePath(), "bin/octave");
		octaveBinCache.setExecutable(true);
		return octaveBinCache;
	}

	public synchronized Boolean getUseNative() {
		if(useNative == null) useNative = false;
		return useNative;
	}


	public synchronized void setUseNative(Boolean useNative) {
		this.useNative = useNative;
	}




	@Override
	public Collection<File> getFiles(IEnvironment forEnv) {
		return archive.getFiles(forEnv);
	}


	public Boolean hasBeenDeployed() {
		if(deployed != null) return deployed;

		synchronized (this) {
			if(deployed == null) deployed = false;
			return deployed;
		}
	}


	public synchronized void setDeployed(Boolean deployed) {
		this.deployed = deployed;
	}


	*/



}
