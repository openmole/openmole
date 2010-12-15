package com.ice.tar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.ice.tar.TarEntry;
import com.ice.tar.TarInputStream;
import com.ice.tar.TarOutputStream;


/**
 * This is a utility class which can create and extract tar or tar.gz files.
 * For tar.gz, the returned tar file would have to be run through the gzip method.
 * 
 * This library is under the Apache License Version 2.0
 * 
 * Authors:
 * 
 * @author jeremy Lucier
 *
 */
public class Tar {

	private static final int BUFFER_SIZE = 1024;
	private static final Logger logger = Logger.getLogger(Tar.class.getName());

	/**
	 * Extracts files from a tar or gzip file to a destination directory
	 * @param srcTarFile
	 * @param destDirectory
	 * @throws IOException
	 */
	public static void extractFiles(File srcTarOrGzFile, File destDirectory) throws IOException {

		// destFolder needs to be a directory
		if(destDirectory.exists() && destDirectory.isFile()) {
			throw new IOException("Destination is not a directory!");
		} else if(destDirectory.exists() == false) {

			// Make the destination directory since it doesn't exist
			destDirectory.mkdir();
		}

		// Src needs to be a file
		if(srcTarOrGzFile.isFile() == false) {
			throw new IOException("Source tar is not a file.");
		}

		// Tar InputStream
		TarInputStream tInputStream = null;
		InputStream secondaryStream = null;
		FileInputStream fileStream = null;

		// File Extension (full name if none)
		String srcFilename = srcTarOrGzFile.getName().toLowerCase();
		String ext = srcFilename.substring((srcFilename.lastIndexOf('.') + 1), srcFilename.length());

		try {
			// Create tar input stream from a .tar.gz file or a normal .tar file
			if(ext.equalsIgnoreCase("gz") && srcFilename.contains("tar.gz")) {

				fileStream = new FileInputStream(srcTarOrGzFile);
				secondaryStream = new GZIPInputStream(fileStream);
				tInputStream = new TarInputStream(secondaryStream);

			} else if(ext.equalsIgnoreCase("tar")) {

				fileStream = new FileInputStream(srcTarOrGzFile);
				tInputStream = new TarInputStream(fileStream);

			} else {
				throw new IOException("Invalid file extension. Supported: tar.gz, tar");
			}


			// Get the first entry in the archive
			TarEntry tarEntry = tInputStream.getNextEntry(); 
			while (tarEntry != null){  

				// Create a file with the same name as the tarEntry 
				File destPath = new File( destDirectory.getAbsolutePath() + File.separatorChar + tarEntry.getName());

				if(logger.isLoggable(Level.FINEST)) {
					logger.log(Level.FINEST, "Extracting " + destPath.getAbsolutePath());
				}

				// If the file is a directory, make all the dir's below it
				if (tarEntry.isDirectory()){
					destPath.mkdirs();                           
				} else {

					// It's a file, grab the containing folder and if it doesn't exist, create it.
					if(destPath.getParentFile().exists() == false) {
						destPath.getParentFile().mkdirs();
					}


					FileOutputStream fOut = new FileOutputStream(destPath); 
					tInputStream.copyEntryContents(fOut);   
					fOut.close();                      
				}

				// Grab the next tarentry
				tarEntry = tInputStream.getNextEntry();
			}    

		} catch(IOException e) {
			throw e;
		} finally {

			// Close out our streams, just to make sure one doesn't get left
			// open for whatever reason.

			if(tInputStream != null) {
				try {
					tInputStream.close();
				} catch(IOException ee) {
					// Ignore...
				}
			}

			if(secondaryStream != null) {
				try {
					secondaryStream.close();
				} catch(IOException ee) {
					// Ignore...
				}
			}

			if(fileStream != null) {
				try {
					fileStream.close();

				} catch(IOException ee) {
					// Ignore...
				}
			}


		}

	}

	/**
	 * Tar's up a directory recursively
	 * @param srcDirectory
	 * @param destTarFile
	 * @throws IOException
	 */
	public static void createDirectoryTar(File srcDirectory, File destTarFile) throws IOException{

		if(destTarFile.getName().toLowerCase().endsWith(".tar") == false) {
			throw new IOException("Destination tar file is not a tar. " + destTarFile.getName().toLowerCase());
		}

		if(srcDirectory.exists() && srcDirectory.isDirectory() == false) {
			throw new IOException("Source directory is not a directory.");
		} else if(srcDirectory.exists() == false) {
			throw new IOException("Source directory does not exist.");
		}

		// We use this output stream to create the Tar file.  Make sure
		// this is set as LONGFILE_GNU -- so we support 8GB+ files and unlimited
		// length filenames.
		FileOutputStream fOut = null;
		TarOutputStream tarOutputStream = null;
		try {

			fOut = new FileOutputStream(destTarFile);
			tarOutputStream = new TarOutputStream(fOut);         

			// Recurse through the directories
			recursiveTar(srcDirectory, srcDirectory, destTarFile, tarOutputStream);

		} catch(IOException e) {
			throw e;
		} finally {

			// Close our output stream, all done!
			if(tarOutputStream != null) {
				try {
					tarOutputStream.close();
				} catch(Exception e) {
					// Ignore..
				}
			}

			if(fOut != null) {
				try {
					fOut.close();
				} catch(Exception e) {
					// Ignore..
				}
			}
		}

	}

	/**
	 * Private method which does the recursion and building of the tar file
	 * @param srcDir
	 * @param destTOS
	 * @throws IOException
	 */
	private static void recursiveTar(File rootDir, File curDir, File destTarFile, TarOutputStream destTOS) throws IOException {

		if(curDir == null || rootDir == null || destTOS == null) {
			return;
		}

		File[] fList = curDir.listFiles();
		if(fList == null) {
			return;
		}

		Queue<File> directories = new LinkedList<File>();

		byte[] buf = new byte[BUFFER_SIZE];

		int fListLen = fList.length;
		File file = null;
		for(int i = 0; i < fListLen; i++) {

			file = fList[i];

			if(file.canRead() == false) {

				logger.info("Could not read file... ");
				if(file.getAbsolutePath() != null) {
					logger.info("Unread File: " + file.getAbsolutePath());
				}
				continue;
			}

			// Directory? Recurse some more.
			if(file.isDirectory()) {

				// Push the directory on to the queue, we process this after adding all the files
				directories.add(file);

			} else {

				// File, let's add it to the Tar.
				String abs = rootDir.getAbsolutePath();
				String fileAbsPath = file.getAbsolutePath();

				// Make sure this file isn't the ISO we're creating, since it'll
				// break badly while we're writing.
				if(destTarFile.getAbsolutePath().equals(fileAbsPath) == false) {

					// We need to set the file's absolute path starting above the root directory
					// Otherwise the tar will have useless folders in them.
					if(fileAbsPath.startsWith(abs)) {
						fileAbsPath = fileAbsPath.substring(abs.length()); 

						// Remove the starting slash if it exists...
						// This covers the C:\ case
						if(fileAbsPath.startsWith(File.separator)) {
							fileAbsPath = fileAbsPath.substring(1);
						}
					}

					logger.info("Adding " + fileAbsPath);

					FileInputStream fis = null;
					try {


						fis = new FileInputStream(file);
						TarEntry te = new TarEntry(fileAbsPath);
						te.setSize(file.length());
						destTOS.putNextEntry(te);
						int count = 0;
						while((count = fis.read(buf, 0, BUFFER_SIZE)) != -1) {
							destTOS.write(buf,0,count);    
						}


					} catch(IOException e) {
						throw e;
					} finally {

						// Close the Tar Output Stream...
						if(destTOS != null) {
							try {
								destTOS.closeEntry();
							} catch(IOException e) {
								// Ignore...
							}
						}

						// Close the file input stream.
						if(fis != null) {
							try {
								fis.close();
							} catch(IOException e) {
								// Ignore...
							}
						}
					}

				} else {
					logger.info("Skipping currently writing archive: " + fileAbsPath);
				}
			}

		}

		// Ensure this gets cleared from memory by removing the 
		// reference before recursing further
		fList = null;

		// Now add the sub-directories
		while(directories.isEmpty() == false) {

			file = directories.poll();
			recursiveTar(rootDir, file, destTarFile, destTOS);  
		}
	}

	/**
	 * Gzip an existing .tar file.  
	 * @param srcTarFile
	 * @param destTarGzFile
	 * @throws IOException
	 */
	public static void gzipTarFile(File srcTarFile, File destTarGzFile) throws IOException {

		FileInputStream inFile = null;
		FileOutputStream outFile = null;
		GZIPOutputStream outGzipFile = null;

		if(srcTarFile.exists() == false) {
			throw new IOException("Source tar file does not exist.");
		}

		if(srcTarFile.getName().toLowerCase().endsWith(".tar") != false) {
			throw new IOException("Source tar file is not a tar.");
		}

		if(destTarGzFile.getName().toLowerCase().endsWith(".tar.gz") != false) {
			throw new IOException("Destination tar.gz file does not end with the proper extension.");
		}

		// Not needed
		/*if(destTarGzFile.exists()) {
			throw new IOException("Destination tar.gz file already exists!");
		}*/

		try { 

			outFile = new FileOutputStream(destTarGzFile);

			// Create the GZIP output stream 
			outGzipFile = new GZIPOutputStream(outFile);

			// Open the input file 
			inFile = new FileInputStream(srcTarFile); 

			// Transfer bytes from the input file to the GZIP output stream 
			byte[] buf = new byte[1024]; 
			int len; 
			while ((len = inFile.read(buf)) > 0) { 
				outGzipFile.write(buf, 0, len); 
			}

			// Complete the GZIP file 
			outGzipFile.finish();

		} catch (IOException e) { 
			throw e;
		} finally {

			// Close all the files

			if(inFile != null) {
				try {
					inFile.close();
				} catch(IOException e) {
					// Ignore
				}
			}

			if(outGzipFile != null) {
				try {
					outGzipFile.close(); 
				} catch(IOException e) {
					// Ignore
				}
			}

			if(outFile != null) {
				try { 
					outFile.close();
				} catch(IOException e) {
					// Ignore
				}
			}
		}
	}


}
