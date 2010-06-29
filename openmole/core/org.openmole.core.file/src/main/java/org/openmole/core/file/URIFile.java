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
package org.openmole.core.file;

import org.openmole.core.file.internal.JSAGAOutputStream;
import org.openmole.core.file.internal.JSAGAInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.DoesNotExistException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.file.FileFactory;
import org.ogf.saga.file.FileInputStream;
import org.ogf.saga.file.FileOutputStream;
import org.ogf.saga.namespace.Flags;
import org.ogf.saga.namespace.NSDirectory;
import org.ogf.saga.namespace.NSEntry;
import org.ogf.saga.namespace.NSFactory;
import org.ogf.saga.task.Task;
import org.ogf.saga.task.TaskMode;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;
import org.openmole.core.batchservicecontrol.BatchStorageDescription;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.file.internal.Activator;
import org.openmole.core.model.file.IURIFile;
import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.filecache.FileCache;
import org.openmole.commons.tools.filecache.FileCacheDeleteOnFinalize;
import org.openmole.commons.tools.filecache.IFileCache;
import org.openmole.commons.tools.io.FileUtil;
import org.openmole.commons.tools.io.StringBuilderOutputStream;
import org.openmole.core.batchservicecontrol.IFailureControl;
import org.openmole.core.batchservicecontrol.IUsageControl;
import org.openmole.misc.workspace.ConfigurationLocation;

import static org.openmole.commons.tools.io.Network.*;

public class URIFile implements IURIFile {

    final static String Group = URIFile.class.getSimpleName();

    final public static ConfigurationLocation Timeout = new ConfigurationLocation(Group, "Timeout");
    final static ConfigurationLocation BufferSize = new ConfigurationLocation(Group, "BufferSize");
    final static ConfigurationLocation CopyTimeout = new ConfigurationLocation(Group, "CopyTimeout");

    static {
       Activator.getWorkspace().addToConfigurations(Timeout, "PT2M");
       Activator.getWorkspace().addToConfigurations(BufferSize, "8192");
       Activator.getWorkspace().addToConfigurations(CopyTimeout, "PT2M");
    }

   // static final long timeout = 2 * 60 * 1000;
    //0.1 KB/S
   // static final int TransfertBuffSize = 10240;
   // static final long TimeOutForTransfert = 100 * 1000;
    final String location;

    public URIFile(File file) throws IOException {
        this(file.getCanonicalFile().toURI().toString());
    }

    public URIFile(String location) {
        this.location = location;
    }

    public URIFile(IURIFile location, String child) throws IOException {
        this(getChild(fromLocation(location.getLocation()), child));
    }

    private URIFile(URL location) {
        this(location.toString());
    }

    public URIFile(URI location) {
        super();

        if (location.getScheme() == null) {
            location = new File(location.getPath()).toURI();
        }
        this.location = location.toString();
    }

    public URIFile(IURIFile file) {
        this(file.getLocation());
    }

    static private URL getChild(URL url, String child) throws IOException {
        if (url.toString().endsWith("/") || child.charAt(0) == '/') {
            return fromLocation(url.toString() + child);
        } else {
            return fromLocation(url.toString() + '/' + child);
        }
    }

    private NSEntry fetchEntry() throws IOException, InterruptedException {

        Task<?, NSEntry> task;
        try {
            task = NSFactory.createNSEntry(TaskMode.ASYNC, Activator.getJSagaSessionService().getSession(), getCachedURL());
        } catch (InternalProcessingError ex) {
            throw new IOException(getLocationString(), ex);
        } catch (NotImplementedException ex) {
            throw new IOException(getLocationString(), ex);
        } catch (NoSuccessException ex) {
            throw new IOException(getLocationString(), ex);
        }


        try {
            return task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new IOException(getLocationString(), e);
        } catch (TimeoutException e) {
            task.cancel(true);
            throw new IOException(getLocationString(), e);
        } catch (InternalProcessingError e) {
            throw new IOException(e);
        }

    }

    private NSDirectory fetchEntryAsDirectory() throws IOException, InterruptedException {
        Task<?, NSDirectory> task;
        try {
            task = NSFactory.createNSDirectory(TaskMode.ASYNC, Activator.getJSagaSessionService().getSession(), getCachedURL());
        } catch (InternalProcessingError ex) {
            throw new IOException(getLocationString(), ex);
        } catch (NotImplementedException ex) {
            throw new IOException(getLocationString(), ex);
        } catch (NoSuccessException ex) {
            throw new IOException(getLocationString(), ex);
        }

        try {
            return task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new IOException(getLocationString(), e);
        } catch (TimeoutException e) {
            task.cancel(true);
            throw new IOException(getLocationString(), e);
        } catch (InternalProcessingError e) {
            throw new IOException(e);
        }

    }

    protected void close(final NSEntry entry) throws InterruptedException {
        Task<?, ?> task;
        try {
            task = entry.close(TaskMode.ASYNC);
        } catch (NotImplementedException ex) {
            Logger.getLogger(URIFile.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        try {
            task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Logger.getLogger(URIFile.class.getName()).log(Level.WARNING, "Error when closing entry for URL " + location, e);
        } catch (TimeoutException e) {
            task.cancel(true);
            Logger.getLogger(URIFile.class.getName()).log(Level.WARNING, "Error when closing entry for URL " + location, e);
        } catch (InternalProcessingError e) {
            Logger.getLogger(URIFile.class.getName()).log(Level.WARNING, "Error when closing entry for URL " + location, e);
        }

    }

    @SoftCachable
    protected URL getCachedURL() throws IOException {
        return fromLocation(location);
    }

    static private URL fromLocation(String location) throws IOException {
        try {
            return URLFactory.createURL(location);
        } catch (BadParameterException e) {
            throw new IOException(location, e);
        } catch (NoSuccessException e) {
            throw new IOException(location, e);
        } catch (NotImplementedException e) {
            throw new IOException(location, e);
        }
    }

    static private URL fromLocation(URI location) throws IOException {
        return fromLocation(location.toString());
    }


    /*-------------------- is a directory ---------------------------*/
    @Override
    public boolean isDirectory() throws IOException, InterruptedException {
        IAccessToken token = getAToken();
        try {
            return isDirectory(token);
        } finally {
            releaseToken(token);
        }
    }

    @Override
    public boolean isDirectory(IAccessToken token) throws IOException, InterruptedException {
        NSEntry entry = fetchEntry();
        try {
            return isDirectory(entry);
        } finally {
            close(entry);
        }
    }

    private boolean isDirectory(final NSEntry entry) throws IOException, InterruptedException {
        Task<?, Boolean> task;
        try {
            task = entry.isDir(TaskMode.ASYNC);
        } catch (NotImplementedException ex) {
            throw new IOException(getLocationString(), ex);
        }

        try {
            return task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new IOException(getLocationString(), e);
        } catch (TimeoutException e) {
            task.cancel(true);
            throw new IOException(getLocationString(), e);
        } catch (InternalProcessingError e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean URLRepresentsADirectory() throws IOException {
        return getLocation().toString().endsWith("/");
    }

    /*--------------------- mkdir ---------------------------*/
    @Override
    public IURIFile mkdir(String name) throws IOException, InterruptedException {
        IAccessToken token = getAToken();
        try {
            return mkdir(name, token);
        } finally {
            releaseToken(token);
        }
    }

    @Override
    public IURIFile mkdir(String name, IAccessToken token) throws IOException, InterruptedException {
        final NSDirectory dir = fetchEntryAsDirectory();
        try {
            String cname;

            if (name.endsWith("/")) {
                cname = name;
            } else {
                cname = name + '/';
            }

            final URL dest = getChild(getCachedURL(), cname);

            Task<?, ?> task;
            try {
                task = dir.makeDir(TaskMode.ASYNC, dest);
            } catch (NotImplementedException ex) {
                throw new IOException(getLocationString(), ex);
            }


            try {
                task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS);
                return new URIFile(this, name);
            } catch (ExecutionException e) {
                throw new IOException(getLocationString(), e);
            } catch (TimeoutException e) {
                task.cancel(true);
                throw new IOException(getLocationString(), e);
            } catch (InternalProcessingError e) {
                throw new IOException(e);
            }
        } finally {
            close(dir);
        }
    }

    @Override
    public IURIFile mkdirIfNotExist(String name) throws IOException, InterruptedException {
        IAccessToken token = getAToken();
        try {
            return mkdirIfNotExist(name, token);
        } finally {
            releaseToken(token);
        }
    }

    @Override
    public IURIFile mkdirIfNotExist(String name, IAccessToken token) throws IOException, InterruptedException {

        try {
            return mkdir(name, token);
        } catch (IOException e) {
            try {
                IURIFile child = getChild(name);
                if (!child.isDirectory(token)) {
                    throw new IOException("Could not create dir " + getLocation(), e);
                }
                return child;
            } catch (IOException e2) {
                getFailureControl().failed();
                throw e2;
            }
        }
    }

    /* ------------------- new file in dir -------------------------*/
    @Override
    public IURIFile newFileInDir(String prefix, String sufix) throws IOException {
        IURIFile ret;
        ret = new URIFile(this, prefix + UUID.randomUUID().toString() + sufix);
        return ret;
    }


    /*-------------------------- exist -------------------------*/
    @Override
    public boolean exist(String name) throws IOException, InterruptedException {
        IAccessToken token = getAToken();
        try {
            return exist(name, token);
        } finally {
            releaseToken(token);
        }
    }

    @Override
    public boolean exist(String name, IAccessToken token) throws IOException, InterruptedException {
        final NSDirectory dir = fetchEntryAsDirectory();

        try {
            final URL dest;
            try {
                dest = URLFactory.createURL(name);
            } catch (NotImplementedException e) {
                throw new IOException("Testing if file " + name + " exist in " + getLocation().toString() + ".", e);
            } catch (BadParameterException e) {
                throw new IOException("Testing if file " + name + " exist in " + getLocation().toString() + ".", e);
            } catch (NoSuccessException e) {
                throw new IOException("Testing if file " + name + " exist in " + getLocation().toString() + ".", e);
            }

            Task<?, Boolean> task;
            try {
                task = dir.exists(TaskMode.ASYNC, dest);
            } catch (NotImplementedException ex) {
                throw new IOException("Testing if file " + name + " exist in " + getLocation().toString() + ".", ex);
            }


            try {
                return task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw new IOException("Testing if file " + name + " exist in " + getLocation().toString() + ".", e);
            } catch (TimeoutException e) {
                task.cancel(true);
                throw new IOException("Testing if file " + name + " exist in " + getLocation().toString() + ".", e);
            } catch (InternalProcessingError e) {
                throw new IOException(e);
            }
        } finally {
            close(dir);
        }
    }

    @Override
    public InputStream openInputStream() throws IOException, InterruptedException {
        IAccessToken token = getAToken();
        try {
            return openInputStream(token);
        } finally {
            releaseToken(token);
        }
    }

    @Override
    public InputStream openInputStream(IAccessToken token) throws IOException, InterruptedException {

        Task<?, FileInputStream> task;
        try {
            task = FileFactory.createFileInputStream(TaskMode.ASYNC, Activator.getJSagaSessionService().getSession(), getCachedURL());
        } catch (NotImplementedException ex) {
            throw new IOException(getLocationString(), ex);
        } catch (NoSuccessException ex) {
            throw new IOException(getLocationString(), ex);
        } catch (InternalProcessingError ex) {
            throw new IOException(getLocationString(), ex);
        }


        try {
            FileInputStream ret = task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS);
            getFailureControl().success();
            return new JSAGAInputStream(ret);
        } catch (ExecutionException e) {
            if (!InternalProcessingError.class.isAssignableFrom(e.getCause().getClass()) || !DoesNotExistException.class.isAssignableFrom(e.getCause().getClass())) {
                getFailureControl().failed();
            }
            throw new IOException(getLocationString(), e);
        } catch (TimeoutException e) {
            task.cancel(true);
            getFailureControl().failed();
            throw new IOException(getLocationString(), e);
        } catch (InternalProcessingError e) {
                throw new IOException(e);
            }
    }

    @Override
    public OutputStream openOutputStream() throws IOException, InterruptedException {
        IAccessToken token = getAToken();
        try {
            return openOutputStream(token);
        } finally {
            releaseToken(token);
        }
    }

    @Override
    public OutputStream openOutputStream(IAccessToken token) throws IOException, InterruptedException {

        Task<?, FileOutputStream> task;
        try {
            task = FileFactory.createFileOutputStream(TaskMode.ASYNC, Activator.getJSagaSessionService().getSession(), getCachedURL(), false);
        } catch (NotImplementedException ex) {
            throw new IOException(getLocationString(), ex);
        } catch (NoSuccessException ex) {
            throw new IOException(getLocationString(), ex);
        } catch (InternalProcessingError ex) {
            throw new IOException(getLocationString(), ex);
        }

        try {
            FileOutputStream ret = task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS);
            getFailureControl().success();
            return new JSAGAOutputStream(ret);
        } catch (ExecutionException e) {
            if (!InternalProcessingError.class.isAssignableFrom(e.getCause().getClass())) {
                getFailureControl().failed();
            }
            throw new IOException(getLocationString(), e);
        } catch (TimeoutException e) {
            task.cancel(true);
            getFailureControl().failed();
            throw new IOException(getLocationString(), e);
        } catch (InternalProcessingError e) {
                throw new IOException(e);
            }
    }

    @Override
    public String getContentAsString() throws IOException, InterruptedException {
        StringBuilder ret = new StringBuilder();

        InputStream is = new java.io.FileInputStream(getFileCache().getFile(true));
        try {
            OutputStream os = new StringBuilderOutputStream(ret);

            try {
                FileUtil.copy(is, os, Activator.getWorkspace().getPreferenceAsInt(BufferSize), Activator.getWorkspace().getPreferenceAsDurationInMs(CopyTimeout));
            } catch (InternalProcessingError e) {
                throw new IOException(e);
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }
        return ret.toString();
    }

    public IFileCache cache() throws IOException, InterruptedException {
        IAccessToken token = getAToken();
        try {
            return cache(token);
        } finally {
            releaseToken(token);
        }
    }


    public synchronized IFileCache cache(IAccessToken token) throws IOException, InterruptedException {
        if (isLocal()) {
            return new FileCache(new File(getCachedURL().getPath()));
        } else {
            try {
                File cacheTmp = Activator.getWorkspace().newTmpFile("file", "cache");
                this.copy(new URIFile(cacheTmp), token);
                return new FileCacheDeleteOnFinalize(cacheTmp);
            } catch (InternalProcessingError e) {
                throw new IOException(getLocationString(), e);
            }
        }
    }

    private boolean isLocal() throws IOException {
        return (getCachedURL().getHost() == null || getCachedURL().getScheme() == null || (getCachedURL().getScheme() != null && getCachedURL().getScheme().compareToIgnoreCase("file") == 0) || IsLocalHost(getCachedURL().getHost()));
    }

    @Override
    public void copy(IURIFile dest) throws IOException, InterruptedException {
        copy(this, dest);
    }

    @Override
    public void copy(IURIFile dest, IAccessToken srcToken) throws IOException, InterruptedException {
        copy(this, srcToken, dest);
    }

    public static void copy(IURIFile src, IAccessToken srcToken, IURIFile dest) throws IOException, InterruptedException {

        IBatchServiceDescription srcDescrption = src.getStorageDescription();
        IBatchServiceDescription destDescrption = dest.getStorageDescription();

        boolean sameRessource = sameRessource(srcDescrption, destDescrption);

        IUsageControl usageControl = Activator.getBatchRessourceControl().getController(destDescrption).getUsageControl();
        IAccessToken destToken;
        if (sameRessource) {
            destToken = srcToken;
        } else {
            destToken = usageControl.waitAToken();
        }

        try {
            copy(src, dest, srcToken, destToken);
        } finally {
            if (!sameRessource) {
                try {
                    usageControl.releaseToken(destToken);
                } catch (InternalProcessingError e) {
                    throw new IOException(e);
                } catch (UserBadDataError e) {
                    throw new IOException(e);
                }

            }
        }
    }

    public static void copy(IURIFile src, IURIFile dest, IAccessToken destToken) throws IOException, InterruptedException {

        IBatchServiceDescription srcDescrption = src.getStorageDescription();
        IBatchServiceDescription destDescrption = dest.getStorageDescription();

        boolean sameRessource = sameRessource(srcDescrption, destDescrption);

        IUsageControl usageControl = Activator.getBatchRessourceControl().getController(srcDescrption).getUsageControl();
        IAccessToken srcToken;
        if (sameRessource) {
            srcToken = destToken;
        } else {
            srcToken = usageControl.waitAToken();
        }

        try {
            copy(src, dest, srcToken, destToken);
        } finally {
            if (!sameRessource) {
                try {
                    usageControl.releaseToken(srcToken);
                } catch (InternalProcessingError e) {
                    throw new IOException(e);
                } catch (UserBadDataError e) {
                    throw new IOException(e);
                }

            }
        }
    }

    public static void copy(final File src, final IURIFile dest) throws IOException, InterruptedException {
        IUsageControl usageControl =  Activator.getBatchRessourceControl().getController(dest.getStorageDescription()).getUsageControl();
        IAccessToken token = usageControl.waitAToken();

        try {
            copy(src, dest, token);
        } finally {
            try {
                usageControl.releaseToken(token);
            } catch (InternalProcessingError e) {
                throw new IOException(e);
            } catch (UserBadDataError e) {
                throw new IOException(e);
            }
        }
    }

    public static void copy(final File src, final IURIFile dest, final IAccessToken token) throws IOException, InterruptedException {

        IFailureControl failureControl = Activator.getBatchRessourceControl().getController(dest.getStorageDescription()).getFailureControl();
        InputStream is = new java.io.FileInputStream(src);
        try {
            OutputStream os = dest.openOutputStream(token);

            try {
                FileUtil.copy(is, os, Activator.getWorkspace().getPreferenceAsInt(BufferSize), Activator.getWorkspace().getPreferenceAsDurationInMs(CopyTimeout));
                failureControl.success();
            } catch(InternalProcessingError e){
                throw new IOException(e);
            }catch (IOException t) {
                failureControl.failed();
                throw t;
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }

    }

    public static void copy(final IURIFile src, final IURIFile dest) throws IOException, InterruptedException {

        IBatchServiceDescription srcDescrption = src.getStorageDescription();
        IBatchServiceDescription destDescrption = dest.getStorageDescription();

        boolean sameRessource = sameRessource(srcDescrption, destDescrption);

        IUsageControl srcUsageControl = Activator.getBatchRessourceControl().getController(srcDescrption).getUsageControl();
        IUsageControl destUsageControl = Activator.getBatchRessourceControl().getController(destDescrption).getUsageControl();
        
        IAccessToken srcToken;
        IAccessToken destToken;

        srcToken = srcUsageControl.waitAToken();
        try {
            if (!sameRessource) {
                destToken = destUsageControl.waitAToken();
            } else {
                destToken = srcToken;
            }
            try {
                copy(src, dest, srcToken, destToken);
            } finally {
                if (!sameRessource) {
                    try {
                        destUsageControl.releaseToken(destToken);
                    } catch (InternalProcessingError e) {
                        throw new IOException(e);
                    } catch (UserBadDataError e) {
                        throw new IOException(e);
                    }
                }
            }
        } finally {
            try {
                srcUsageControl.releaseToken(srcToken);
            } catch (InternalProcessingError e) {
                throw new IOException(e);
            } catch (UserBadDataError e) {
                throw new IOException(e);
            }
        }

    }

    private static void copy(final IURIFile src, final IURIFile dest, IAccessToken srcToken, IAccessToken destToken) throws IOException, InterruptedException {
        boolean sameRessource = sameRessource(src.getStorageDescription(), dest.getStorageDescription());

        IFailureControl srcFailureControl = Activator.getBatchRessourceControl().getController(src.getStorageDescription()).getFailureControl();
        IFailureControl destFailureControl = Activator.getBatchRessourceControl().getController(dest.getStorageDescription()).getFailureControl();
        
        InputStream is = src.openInputStream(srcToken);
        try {
            OutputStream os = dest.openOutputStream(destToken);

            try {
                FileUtil.copy(is, os, Activator.getWorkspace().getPreferenceAsInt(BufferSize), Activator.getWorkspace().getPreferenceAsDurationInMs(CopyTimeout));
                srcFailureControl.success();
                if (!sameRessource) {
                    destFailureControl.success();
                }
            } catch(InternalProcessingError e) {
                throw new IOException(e);
            }catch (IOException t) {
                srcFailureControl.failed();
                if (!sameRessource) {
                    destFailureControl.failed();
                }
                throw t;
            } finally {
                os.close();
            }
        } finally {
            is.close();
        }
    }

    private static boolean sameRessource(IBatchServiceDescription srcDescrption, IBatchServiceDescription destDescrption) {
        return srcDescrption.equals(destDescrption);
    }

    /* -------------------- remove -------------------------------*/
    @Override
    public void remove(boolean recursive) throws IOException, InterruptedException {
        remove(true, recursive);
    }

    @Override
    public void remove(boolean recursive, IAccessToken token) throws IOException, InterruptedException {
        remove(true, recursive, token);
    }

    @Override
    public void remove(boolean timeOut, boolean recursive) throws IOException, InterruptedException {
        IAccessToken token = getAToken();
        try {
            remove(timeOut, recursive, token);
        } finally {
            releaseToken(token);
        }
    }

    @Override
    public void remove(boolean timeOut, final boolean recursive, final IAccessToken token) throws IOException, InterruptedException {
        final NSEntry entry = fetchEntry();
        try {

            Task<?, ?> task;
            try {
                if (recursive /*&& directory*/) {
                    task = entry.remove(TaskMode.ASYNC, Flags.RECURSIVE.getValue());
                } else {
                    task = entry.remove(TaskMode.ASYNC);
                }

            } catch (NotImplementedException ex) {
                throw new IOException(getLocationString(), ex);
            }

            try {
                if (timeOut) {
                    task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS);
                } else {
                    task.get();
                }
            } catch (ExecutionException e) {
                throw new IOException(getLocationString(), e);
            } catch (TimeoutException e) {
                task.cancel(true);
                throw new IOException(getLocationString(), e);
            } catch (InternalProcessingError e) {
                throw new IOException(e);
            }
        } finally {
            close(entry);
        }

    }

    @Override
    public List<String> list() throws IOException, InterruptedException {
        IAccessToken token = getAToken();
        try {
            return list(token);
        } finally {
            releaseToken(token);
        }
    }

    @Override
    public List<String> list(IAccessToken token) throws IOException, InterruptedException {

        List<String> ret;
        final NSDirectory dir = fetchEntryAsDirectory();

        try {
            Task<?, List<URL>> task;
            try {
                task = dir.list(TaskMode.ASYNC);
            } catch (NotImplementedException ex) {
                throw new IOException(getLocationString(), ex);
            }


            try {
                List<URL> urls = task.get(Activator.getWorkspace().getPreferenceAsDurationInMs(Timeout), TimeUnit.MILLISECONDS);
                ret = new ArrayList<String>(urls.size());

                for (URL url : urls) {
                    ret.add(url.toString());
                }

                return ret;
            } catch (InternalProcessingError e) {
                throw new IOException(e);
            }catch (ExecutionException e) {
                throw new IOException(getLocationString(), e);
            } catch (TimeoutException e) {
                task.cancel(true);
                throw new IOException(getLocationString(), e);
            }
        } finally {
            close(dir);
        }
    }

    @Override
    public URIFile getChild(String child) throws IOException {
        return new URIFile(this, child);
    }

    @Override
    public String toString() {
        return location.toString();
    }

    public IAccessToken getAToken() throws InterruptedException {
        return getUsageControl().waitAToken();
    }

    public void releaseToken(IAccessToken token) throws IOException {
        try {
            getUsageControl().releaseToken(token);
        } catch (InternalProcessingError ex) {
            throw new IOException(ex);
        } catch (UserBadDataError ex) {
            throw new IOException(ex);
        }
    }

    @SoftCachable
    @Override
    public IBatchServiceDescription getStorageDescription() {
        return new BatchStorageDescription(getLocation());
    }

    @Override
    @SoftCachable
    public URI getLocation() {
        return URI.create(getLocationString());
    }

    @Override
    public String getLocationString() {
        return location;
    }


    @Override
    public IFileCache getFileCache() throws IOException, InterruptedException {
        try {
            return Activator.getFileCache().getURIFileCache(this, this);
        } catch (InternalProcessingError ex) {
            throw new IOException(getLocationString(), ex);
        }
    }

    @Override
    public IFileCache getFileCache(IAccessToken token) throws IOException, InterruptedException {
        try {
            return Activator.getFileCache().getURIFileCache(this, this, token);
        } catch (InternalProcessingError ex) {
            throw new IOException(getLocationString(), ex);
        }
    }

    @Override
    public int compareTo(IURIFile o) {
        return getLocationString().compareTo(o.getLocationString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final URIFile other = (URIFile) obj;
        if (this.location != other.location && (this.location == null || !this.location.equals(other.location))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.location != null ? this.location.hashCode() : 0);
        return hash;
    }
    
    private IUsageControl getUsageControl() {
        return Activator.getBatchRessourceControl().getController(getStorageDescription()).getUsageControl();
    }
    
    private IFailureControl getFailureControl() {
        return Activator.getBatchRessourceControl().getController(getStorageDescription()).getFailureControl();
    }
}
