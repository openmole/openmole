/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.model.file;

import java.io.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;
import java.util.List;

import org.openmole.core.model.execution.batch.IAccessToken;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;


public interface IURIFile extends Comparable<IURIFile> {

	boolean isDirectory() throws IOException, InterruptedException;
	boolean isDirectory(IAccessToken token) throws IOException, InterruptedException;
	boolean URLRepresentsADirectory() throws IOException;

	IURIFile mkdir(String name) throws IOException, InterruptedException;
	IURIFile mkdir(String name, IAccessToken token) throws IOException, InterruptedException;
	IURIFile mkdirIfNotExist(String name) throws IOException, InterruptedException;
	IURIFile mkdirIfNotExist(String name, IAccessToken token) throws IOException, InterruptedException;

	IURIFile newFileInDir(String prefix, String sufix) throws IOException, InterruptedException;
	//IURIFile newFileInDir(String prefix, String sufix, IAccessToken token) throws IOException;
	
        InputStream openInputStream() throws IOException, InterruptedException;
	InputStream openInputStream(IAccessToken token) throws IOException, InterruptedException;

	OutputStream openOutputStream() throws IOException, InterruptedException;
	OutputStream openOutputStream(IAccessToken token) throws IOException, InterruptedException;

	String getContentAsString() throws IOException, InterruptedException;
	
	void copy(IURIFile dest) throws IOException, InterruptedException;
	void copy(IURIFile dest, IAccessToken srcToken) throws IOException, InterruptedException;

	void remove(boolean recusrsive) throws IOException, InterruptedException;
	void remove(boolean timeOut,boolean recusrsive) throws IOException, InterruptedException;
	void remove(boolean recursive, IAccessToken token) throws IOException, InterruptedException;
	void remove(boolean timeOut, boolean recursive, IAccessToken token) throws IOException, InterruptedException;

	List<String> list() throws IOException, InterruptedException;
	List<String> list(IAccessToken token) throws IOException, InterruptedException;

	boolean exist(String name) throws IOException, InterruptedException;
	boolean exist(String name, IAccessToken token) throws IOException, InterruptedException;
        
	//IHash getHash() throws IOException, InterruptedException;
       // IHash getHash(IAccessToken token) throws IOException, InterruptedException;

        //IHash getHash(Object duration) throws IOException, InterruptedException;
        //IHash getHash(IAccessToken token, Object duration) throws IOException, InterruptedException;
        
	File getFile() throws IOException, InterruptedException;
        File getFile(IAccessToken token) throws IOException, InterruptedException;
        
        File getFile(Object duration) throws IOException, InterruptedException;
        File getFile(IAccessToken token, Object duration) throws IOException, InterruptedException;

	IURIFile getChild(String child) throws IOException;

	URI getLocation();
        String getLocationString();

	IBatchServiceDescription getStorageDescription();
	

}