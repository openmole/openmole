package com.ice.tar;

import java.io.File;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * This class represents an entry in a Tar archive. It consists of the entry's
 * header, as well as the entry's File. Entries can be instantiated in one of
 * three ways, depending on how they are to be used.
 * <p>
 * TarEntries that are created from the header bytes read from an archive are
 * instantiated with the TarEntry( byte[] ) constructor. These entries will be
 * used when extracting from or listing the contents of an archive. These
 * entries have their header filled in using the header bytes. They also set the
 * File to null, since they reference an archive entry not a file.
 * <p>
 * TarEntries that are created from Files that are to be written into an archive
 * are instantiated with the TarEntry( File ) constructor. These entries have
 * their header filled in using the File's information. They also keep a
 * reference to the File for convenience when writing entries.
 * <p>
 * Finally, TarEntries can be constructed from nothing but a name. This allows
 * the programmer to construct the entry by hand, for instance when only an
 * InputStream is available for writing to the archive, and the header
 * information is constructed from other information. In this case the header
 * fields are set to defaults and the File is set to null.
 * 
 * <pre>
 * 
 * Original Unix Tar Header:
 * 
 * Field  Field     Field
 * Width  Name      Meaning
 * -----  --------- ---------------------------
 *   100  name      name of file
 *     8  mode      file mode
 *     8  uid       owner user ID
 *     8  gid       owner group ID
 *    12  size      length of file in bytes
 *    12  mtime     modify time of file
 *     8  chksum    checksum for header
 *     1  link      indicator for links
 *   100  linkname  name of linked file
 * 
 * </pre>
 * 
 * <pre>
 * 
 * POSIX "ustar" Style Tar Header:
 * 
 * Field  Field     Field
 * Width  Name      Meaning
 * -----  --------- ---------------------------
 *   100  name      name of file
 *     8  mode      file mode
 *     8  uid       owner user ID
 *     8  gid       owner group ID
 *    12  size      length of file in bytes
 *    12  mtime     modify time of file
 *     8  chksum    checksum for header
 *     1  typeflag  type of file
 *   100  linkname  name of linked file
 *     6  magic     USTAR indicator
 *     2  version   USTAR version
 *    32  uname     owner user name
 *    32  gname     owner group name
 *     8  devmajor  device major number
 *     8  devminor  device minor number
 *   155  prefix    prefix for file name
 * 
 * struct posix_header
 *   {                     byte offset
 *   char name[100];            0
 *   char mode[8];            100
 *   char uid[8];             108
 *   char gid[8];             116
 *   char size[12];           124
 *   char mtime[12];          136
 *   char chksum[8];          148
 *   char typeflag;           156
 *   char linkname[100];      157
 *   char magic[6];           257
 *   char version[2];         263
 *   char uname[32];          265
 *   char gname[32];          297
 *   char devmajor[8];        329
 *   char devminor[8];        337
 *   char prefix[155];        345
 *   };                       500
 * 
 * </pre>
 * 
 * This library is under the Apache License Version 2.0
 * 
 * Authors:
 * 
 * @author Jeremy Lucier
 * @author Timothy Gerard Endres (Original Author)
 * 
 */

public class TarEntry extends Object implements Cloneable {

	private static final Logger logger = Logger.getLogger(TarEntry.class.getName());

	/** Format choices **/
	public static final int GNU_FORMAT = 0;
	public static final int USTAR_FORMAT = 1;
	public static final int UNIX_FORMAT = 2;

	/** Maximum length of a user's name in the tar file */
	public static final int MAX_NAMELEN = 31;

	/** Default permissions bits for directories */
	public static final int DEFAULT_DIR_MODE = 040755;

	/** Default permissions bits for files */
	public static final int DEFAULT_FILE_MODE = 0100644;

	/** Convert millis to seconds */
	public static final int MILLIS_PER_SECOND = 1000; 

	/** If this entry represents a File, this references it. */
	private File file;

	// HEADER VARS
	/** The entry's name. */
	private String name;

	/** The entry's permission mode. */
	private int mode;

	/** The entry's user id. */
	private int userId;

	/** The entry's group id. */
	private int groupId;

	/** The entry's size. */
	private long size;

	/** The entry's modification time. */
	private long modTime;

	/** The entry's checksum. */
	@SuppressWarnings("unused")
	private int checkSum;

	/** The entry's link flag. */
	private byte linkFlag;

	/** The entry's link name. */
	private String linkName;

	/** The entry's magic tag. */
	private String magic;

	/** The entry's user name. */
	private String userName;

	/** The entry's group name. */
	private String groupName;

	/** The entry's major device number. */
	private int devMajor;

	/** The entry's minor device number. */
	private int devMinor;

	/**
	 * The entry's offset for multi volume this is filesize-part written to last
	 * file
	 */
	@SuppressWarnings("unused")
	private long offB;

	private int tarFormat = GNU_FORMAT;

	// END HEADER VARS

	/** The default constructor is protected for use only by subclasses. */
	private TarEntry() {

		// Set the defaults
		this.magic = TarConstants.OLDGNU_TMAGIC;
		this.name = null;
		this.linkName = "";

		String user = System.getProperty("user.name", "");
		if (user.length() > MAX_NAMELEN) {
			user = user.substring(0, MAX_NAMELEN);
		}

		this.userId = 0;
		this.groupId = 0;
		this.userName = user;
		this.groupName = "";
		this.file = null;
	}

	/**
	 * Construct an entry from an archive's header bytes. File is set to null.
	 * 
	 * @param headerBuf
	 *            The header bytes from a tar archive entry.
	 */
	public TarEntry(byte[] headerBuf) throws InvalidHeaderException {
		this();
		this.parseTarHeader(headerBuf);
	}

	/**
	 * Construct an entry for a file. File is set to file, and the header is
	 * constructed from information from the file.
	 * 
	 * @param file
	 *            The file that the entry represents.
	 */
	public TarEntry(File file) throws InvalidHeaderException {
		this();
		this.parseFileTarHeader(file);
	}

	/**
	 * Construct an entry with only a name. This allows the programmer to
	 * construct the entry's header "by hand". File is set to null.
	 */
	public TarEntry(String name) {
		this();
		this.nameTarHeader(name);
	}

	public TarEntry(String name, byte linkFlag) {
		this(name);
		this.linkFlag = linkFlag;
		if (this.linkFlag == TarConstants.LF_GNUTYPE_LONGNAME) {
			this.magic = TarConstants.OLDGNU_TMAGIC;
		}
	}

	/*
	 * @Override public Object clone() { TarEntry entry = null;
	 * 
	 * try { entry = (TarEntry) super.clone();
	 * 
	 * if (this != null) { entry.header = (TarHeader) this.clone(); }
	 * 
	 * if (this.file != null) { entry.file = new
	 * File(this.file.getAbsolutePath()); } } catch (CloneNotSupportedException
	 * ex) { ex.printStackTrace(System.err); }
	 * 
	 * return entry; }
	 */

	/**
	 * Determine if the two entries are equal. Equality is determined by the
	 * header names being equal.
	 * 
	 * @return it Entry to be checked for equality.
	 * @return True if the entries are equal.
	 */
	public boolean equals(TarEntry it) {
		return this.name.equals(it.getName());
	}

	/**
	 * If this entry represents a file, and the file is a directory, return an
	 * array of TarEntries for this entry's children.
	 * 
	 * @return An array of TarEntry's for this entry's children.
	 */
	public TarEntry[] getDirectoryEntries() throws InvalidHeaderException {
		if (this.file == null || !this.file.isDirectory()) {
			return new TarEntry[0];
		}

		String[] list = this.file.list();

		TarEntry[] result = new TarEntry[list.length];

		int listLen = list.length;
		for (int i = 0; i < listLen; ++i) {
			result[i] = new TarEntry(new File(this.file, list[i]));
		}

		return result;
	}

	/**
	 * Get this entry's file.
	 * 
	 * @return This entry's file.
	 */
	public File getFile() {
		return this.file;
	}

	/**
	 * Get this entry's mode.
	 *
	 * @return This entry's mode.
	 */
	public int getMode() {
		return this.mode;
	} 


	/**
	 * Get this entry's group id.
	 * 
	 * @return This entry's group id.
	 */
	public int getGroupId() {
		return this.groupId;
	}

	/**
	 * Get this entry's group name.
	 * 
	 * @return This entry's group name.
	 */
	public String getGroupName() {
		return this.groupName;
	}

	/**
	 * Set this entry's modification time.
	 * 
	 * @param time
	 *            This entry's new modification time.
	 */
	public Date getModTime() {
		return new Date(this.modTime * MILLIS_PER_SECOND);
	}

	/**
	 * Get this entry's name.
	 * 
	 * @return This entry's name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Get this entry's file size.
	 * 
	 * @return This entry's file size.
	 */
	public long getSize() {
		return this.size;
	}

	/**
	 * Get the tar format(GNU_FORMAT, UNIX_FORMAT, USTAR_FORMAT)
	 * 
	 * @return
	 */
	public int getTarFormat() {
		return tarFormat;
	}

	/**
	 * Get this entry's link name.
	 *
	 * @return This entry's link name.
	 */
	public String getLinkName() {
		return linkName.toString();
	} 


	/**
	 * Get this entry's user id.
	 * 
	 * @return This entry's user id.
	 */
	public int getUserId() {
		return this.userId;
	}

	/**
	 * Get this entry's user name.
	 * 
	 * @return This entry's user name.
	 */
	public String getUserName() {
		return this.userName;
	}

	/**
	 * Determine if the given entry is a descendant of this entry. Descendancy
	 * is determined by the name of the descendant starting with this entry's
	 * name.
	 * 
	 * @param desc
	 *            Entry to be checked as a descendent of this.
	 * @return True if entry is a descendant of this.
	 */
	public boolean isDescendent(TarEntry desc) {
		return desc.getName().startsWith(this.name);
	}

	/**
	 * Return whether or not this entry represents a directory.
	 * 
	 * @return True if this entry is a directory.
	 */
	public boolean isDirectory() {
		if (this.file != null) {
			return this.file.isDirectory();
		}

		if (this != null) {
			if (this.linkFlag == TarConstants.LF_DIR) {
				return true;
			}

			if (this.name.endsWith("/")) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Indicate if this entry is a GNU long name block
	 * 
	 * @return true if this is a long name extension provided by GNU tar
	 */
	public boolean isGNULongNameEntry() {
		return this.linkFlag == TarConstants.LF_GNUTYPE_LONGNAME
		&& this.name.equals(TarConstants.GNU_LONGLINK);
	}

	/**
	 * Fill in a TarHeader given only the entry's name.
	 * 
	 * @param hdr
	 *            The TarHeader to fill in.
	 * @param name
	 *            The tar entry name.
	 */
	public void nameTarHeader(String name) {
		boolean isDir = name.endsWith("/");

		this.checkSum = 0;
		this.devMajor = 0;
		this.devMinor = 0;

		this.name = name;
		this.mode = isDir ? DEFAULT_DIR_MODE : DEFAULT_FILE_MODE;
		this.userId = 0;
		this.groupId = 0;
		this.size = 0;
		this.checkSum = 0;

		this.modTime = new java.util.Date().getTime() / MILLIS_PER_SECOND;

		this.linkFlag = isDir ? TarConstants.LF_DIR : TarConstants.LF_NORMAL;

		this.linkName = "";
		this.userName = "";
		this.groupName = "";

		this.devMajor = 0;
		this.devMinor = 0;
	}

	/**
	 * Fill in a TarHeader with information from a File.
	 * 
	 * @param hdr
	 *            The TarHeader to fill in.
	 * @param file
	 *            The file from which to get the header information.
	 */
	public void parseFileTarHeader(File file) throws InvalidHeaderException {

		this.file = file;

		String name = file.getPath();
		String osname = System.getProperty("os.name");
		if (osname != null) {
			// Strip off drive letters!
			// REVIEW Would a better check be "(File.separator == '\')"?

			// String Win32Prefix = "Windows";
			// String prefix = osname.substring( 0, Win32Prefix.length() );
			// if ( prefix.equalsIgnoreCase( Win32Prefix ) )

			// if ( File.separatorChar == '\\' )

			// Windows OS check was contributed by
			// Patrick Beard <beard@netscape.com>
			String Win32Prefix = "windows";
			if (osname.toLowerCase().startsWith(Win32Prefix)) {
				if (name.length() > 2) {
					char ch1 = name.charAt(0);
					char ch2 = name.charAt(1);
					if (ch2 == ':'
						&& (ch1 >= 'a' && ch1 <= 'z' || ch1 >= 'A'
							&& ch1 <= 'Z')) {
						name = name.substring(2);
					}
				}
			}
		}

		name = name.replace(File.separatorChar, '/');

		// No absolute pathnames
		// Windows (and Posix?) paths can start with "\\NetworkDrive\",
		// so we loop on starting /'s.

		while (name.startsWith("/")) {
			name = name.substring(1);
		}

		this.linkName = "";

		StringBuilder hdrName = new StringBuilder(name);

		if (file.isDirectory()) {
			this.size = 0;
			this.mode = DEFAULT_DIR_MODE;
			this.linkFlag = TarConstants.LF_DIR;
			if (this.name.charAt(this.name.length() - 1) != '/') {
				hdrName.append("/");
			}
		} else {
			this.size = file.length();
			this.mode = DEFAULT_FILE_MODE;
			this.linkFlag = TarConstants.LF_NORMAL;
		}

		this.name = hdrName.toString();

		// UNDONE When File lets us get the userName, use it!

		this.modTime = file.lastModified() / MILLIS_PER_SECOND;
		this.checkSum = 0;
		this.devMajor = 0;
		this.devMinor = 0;
	}

	/**
	 * Parse an entry's TarHeader information from a header buffer.
	 * 
	 * Old unix-style code contributed by David Mehringer
	 * <dmehring@astro.uiuc.edu>.
	 * 
	 * @param header
	 *            The tar entry header buffer to get information from.
	 */
	public void parseTarHeader(byte[] headerBuf) throws InvalidHeaderException {

		int offset = 0;

		//
		// NOTE Recognize archive header format.
		//

		// Unix format magic (no magic): 00000
		if (headerBuf[257] == 0 && headerBuf[258] == 0 && headerBuf[259] == 0
				&& headerBuf[260] == 0 && headerBuf[261] == 0) {

			this.tarFormat = UNIX_FORMAT;

		} else if (headerBuf[257] == 'u' && headerBuf[258] == 's'
			&& headerBuf[259] == 't' && headerBuf[260] == 'a'
				&& headerBuf[261] == 'r' && headerBuf[262] == 0) {
			// Ustar format magic: "ustar\0"

			// Posix and new gnutar should fall into thie category due to their magic being:
			// typically "ustar\000"

			this.tarFormat = USTAR_FORMAT;

		} else if (headerBuf[257] == 'u' && headerBuf[258] == 's'
			&& headerBuf[259] == 't' && headerBuf[260] == 'a'
				&& headerBuf[261] == 'r' && headerBuf[262] != 0
				&& headerBuf[263] != 0) {
			// [old and new] GNUTar format magic: "ustar[wild][wild]" (typically "  ")
			this.tarFormat = GNU_FORMAT;

		} else if (headerBuf[257] == 'u' && headerBuf[258] == 's'
			&& headerBuf[259] == 't' && headerBuf[260] == 'a'
				&& headerBuf[261] == 'r' && headerBuf[262] == 32
				&& headerBuf[263] == 0) {

			// GNUTar Format: "ustar \0".  This technically shouldn't
			// be used, but it's a safety check for bad flags
			// TODO: REVIEW this is GNUtar format
			this.tarFormat = GNU_FORMAT;

		} else {
			StringBuffer buf = new StringBuffer(128);

			buf.append("header magic is not 'ustar' or unix-style zeros, it is '");
			buf.append(headerBuf[257]);
			buf.append(headerBuf[258]);
			buf.append(headerBuf[259]);
			buf.append(headerBuf[260]);
			buf.append(headerBuf[261]);
			buf.append(headerBuf[262]);
			buf.append(headerBuf[263]);
			buf.append("', or (dec) ");
			buf.append(headerBuf[257]);
			buf.append(", ");
			buf.append(headerBuf[258]);
			buf.append(", ");
			buf.append(headerBuf[259]);
			buf.append(", ");
			buf.append(headerBuf[260]);
			buf.append(", ");
			buf.append(headerBuf[261]);
			buf.append(", ");
			buf.append(headerBuf[262]);
			buf.append(", ");
			buf.append(headerBuf[263]);

			throw new InvalidHeaderException(buf.toString());
		}

		this.name = TarFileUtil.parseFileName(headerBuf);

		offset = TarConstants.NAMELEN;

		this.mode = (int) TarFileUtil.parseOctal(headerBuf, offset,
				TarConstants.MODELEN);

		offset += TarConstants.MODELEN;

		this.userId = (int) TarFileUtil.parseOctal(headerBuf, offset,
				TarConstants.UIDLEN);

		offset += TarConstants.UIDLEN;

		this.groupId = (int) TarFileUtil.parseOctal(headerBuf, offset,
				TarConstants.GIDLEN);

		offset += TarConstants.GIDLEN;

		if (this.tarFormat == GNU_FORMAT) {
			this.size = TarFileUtil.getSize(headerBuf, offset,
					TarConstants.SIZELEN);
		} else {
			this.size = TarFileUtil.parseOctal(headerBuf, offset,
					TarConstants.SIZELEN);
		}

		offset += TarConstants.SIZELEN;

		this.modTime = TarFileUtil.parseOctal(headerBuf, offset,
				TarConstants.MODTIMELEN);

		offset += TarConstants.MODTIMELEN;

		this.checkSum = (int) TarFileUtil.parseOctal(headerBuf, offset,
				TarConstants.CHKSUMLEN);

		offset += TarConstants.CHKSUMLEN;

		this.linkFlag = headerBuf[offset++];

		this.linkName = TarFileUtil.parseName(headerBuf, offset,
				TarConstants.NAMELEN);

		offset += TarConstants.NAMELEN;

		if (this.tarFormat == USTAR_FORMAT) {

			this.magic = TarFileUtil.parseName(headerBuf, offset,
					TarConstants.MAGICLEN);

			offset += TarConstants.MAGICLEN;

			this.userName = TarFileUtil.parseName(headerBuf, offset,
					TarConstants.UNAMELEN);

			offset += TarConstants.UNAMELEN;

			this.groupName = TarFileUtil.parseName(headerBuf, offset,
					TarConstants.GNAMELEN);

			offset += TarConstants.GNAMELEN;

			this.devMajor = (int) TarFileUtil.parseOctal(headerBuf, offset,
					TarConstants.DEVLEN);

			offset += TarConstants.DEVLEN;

			this.devMinor = (int) TarFileUtil.parseOctal(headerBuf, offset,
					TarConstants.DEVLEN);

			// Gets the offset and sets the offset variable in the header
			this.offB = TarFileUtil.parseOctal(headerBuf, 369, 12);

		} else {

			this.devMajor = 0;
			this.devMinor = 0;
			this.magic = "";
			this.userName = "";
			this.groupName = "";

		}
	}

	/**
	 * Set this entry's group id.
	 * 
	 * @param groupId
	 *            This entry's new group id.
	 */
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	/**
	 * Set this entry's group name.
	 * 
	 * @param groupName
	 *            This entry's new group name.
	 */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/**
	 * Convenience method to set this entry's group and user ids.
	 * 
	 * @param userId
	 *            This entry's new user id.
	 * @param groupId
	 *            This entry's new group id.
	 */
	public void setIds(int userId, int groupId) {
		this.setUserId(userId);
		this.setGroupId(groupId);
	}

	/**
	 * Set this entry's modification time.
	 * 
	 * @param time
	 *            This entry's new modification time.
	 */
	public void setModTime(Date time) {
		this.modTime = time.getTime() / MILLIS_PER_SECOND;
	}

	/**
	 * Set this entry's modification time. The parameter passed to this method
	 * is in "Java time".
	 * 
	 * @param time
	 *            This entry's new modification time.
	 */
	public void setModTime(long time) {
		this.modTime = time / MILLIS_PER_SECOND;
	}

	/**
	 * Set this entry's name.
	 * 
	 * @param name
	 *            This entry's new name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Set the mode for this entry
	 *
	 * @param mode the mode for this entry
	 */
	public void setMode(int mode) {
		this.mode = mode;
	} 

	/**
	 * Convenience method to set this entry's group and user names.
	 * 
	 * @param userName
	 *            This entry's new user name.
	 * @param groupName
	 *            This entry's new group name.
	 */
	public void setNames(String userName, String groupName) {
		this.setUserName(userName);
		this.setGroupName(groupName);
	}

	/**
	 * Set this entry's file size.
	 * 
	 * @param size
	 *            This entry's new file size.
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * Set the tar format(GNU_FORMAT, UNIX_FORMAT, USTAR_FORMAT)
	 * 
	 * @param tarFormat
	 * @throws Exception
	 */
	public void setTarFormat(int tarFormat) throws Exception {

		if (tarFormat != GNU_FORMAT && tarFormat != UNIX_FORMAT
				&& tarFormat != USTAR_FORMAT) {
			throw new Exception("Invalid format specified.");
		}

		// Set this tar as the requested format
		this.tarFormat = tarFormat;

		// Set the magic type
		if (tarFormat == UNIX_FORMAT) {
			this.magic = "";
		} else if (tarFormat == USTAR_FORMAT) {
			this.magic = TarConstants.TMAGIC;
		} else if (tarFormat == GNU_FORMAT) {
			this.magic = TarConstants.OLDGNU_TMAGIC;
		}
	}

	/**
	 * Set this entry's user id.
	 * 
	 * @param userId
	 *            This entry's new user id.
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}

	/**
	 * Set this entry's user name.
	 * 
	 * @param userName
	 *            This entry's new user name.
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(128);
		return result.append("[TarEntry name=").append(this.getName()).append(
		", isDir=").append(this.isDirectory()).append(", size=")
		.append(this.getSize()).append(", userId=").append(
				this.getUserId()).append(", user=").append(
						this.getUserName()).append(", groupId=").append(
								this.getGroupId()).append(", group=").append(
										this.getGroupName()).append("]").toString();
	}

	/**
	 * Write an entry's header information to a header buffer. This method can
	 * throw an InvalidHeaderException
	 * 
	 * @param outbuf
	 *            The tar entry header buffer to fill in.
	 * @throws InvalidHeaderException
	 *             If the name will not fit in the header.
	 */
	public void writeEntryHeader(byte[] outbuf) throws InvalidHeaderException {
		int offset = 0;

		if (this.tarFormat == UNIX_FORMAT && this.name.length() > 100) {
			throw new InvalidHeaderException(
					"file path is greater than 100 characters, " + this.name);
		}

		offset = TarFileUtil.getFileNameBytes(this.name, outbuf,
				(this.tarFormat == GNU_FORMAT));

		offset = TarFileUtil.getOctalBytes(this.mode, outbuf, offset,
				TarConstants.MODELEN, TarConstants.ZERO_BYTE);

		offset = TarFileUtil.getOctalBytes(this.userId, outbuf, offset,
				TarConstants.UIDLEN, TarConstants.ZERO_BYTE);

		offset = TarFileUtil.getOctalBytes(this.groupId, outbuf, offset,
				TarConstants.GIDLEN, TarConstants.ZERO_BYTE);

		long size = this.size;

		if (logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "Size is at: " + offset);
		}

		offset = TarFileUtil.getLongOctalBytes(size, outbuf, offset,
				TarConstants.SIZELEN);

		offset = TarFileUtil.getLongOctalBytes(this.modTime, outbuf, offset,
				TarConstants.MODTIMELEN);

		int csOffset = offset;
		for (int c = 0; c < TarConstants.CHKSUMLEN; ++c) {
			outbuf[offset++] = TarConstants.SPACER_BYTE;
		}

		outbuf[offset++] = this.linkFlag;

		offset = TarFileUtil.getNameBytes(this.linkName, outbuf, offset,
				TarConstants.NAMELEN);

		if (this.tarFormat == UNIX_FORMAT) {
			for (int i = 0; i < TarConstants.MAGICLEN; ++i) {
				outbuf[offset++] = 0;
			}
		} else {

			offset = TarFileUtil.getNameBytes(this.magic, outbuf, offset,
					TarConstants.MAGICLEN);
			
			if (this.tarFormat == USTAR_FORMAT) {
				outbuf[offset - 2] = TarConstants.ZERO_BYTE;
				outbuf[offset - 1] = TarConstants.ZERO_BYTE;
			}
		}

		offset = TarFileUtil.getNameBytes(this.userName, outbuf, offset,
				TarConstants.UNAMELEN);

		offset = TarFileUtil.getNameBytes(this.groupName, outbuf, offset,
				TarConstants.GNAMELEN);

		offset = TarFileUtil.getOctalBytes(this.devMajor, outbuf, offset,
				TarConstants.DEVLEN, TarConstants.ZERO_BYTE);

		offset = TarFileUtil.getOctalBytes(this.devMinor, outbuf, offset,
				TarConstants.DEVLEN, TarConstants.ZERO_BYTE);

		while (offset < outbuf.length) {
			outbuf[offset++] = 0;
		}

		/*
		 * This sets the real size if the size is bigger than the 8GB barrier.
		 * Not supported by USTAR 
		 * (based on http://en.wikipedia.org/wiki/Tar_%28file_format%29#UStar_format)
		 * 
		 * Directory "size" fix contributed by: Bert Becker
		 * <becker@informatik.hu-berlin.de>
		 */
		if(this.tarFormat != USTAR_FORMAT) {
			if (size > 8589934592l) {
				offset = TarFileUtil.setRealSize(size, outbuf, 124, 12);
			}
		}

		long checkSum = TarFileUtil.computeCheckSum(outbuf);

		TarFileUtil.getCheckSumOctalBytes(checkSum, outbuf, csOffset,
				TarConstants.CHKSUMLEN);
	}

	/**
	 * Write an entry's header information to a header buffer. This method can
	 * throw an InvalidHeaderException
	 * 
	 * @param outbuf
	 *            The tar entry header buffer to fill in.
	 * @throws InvalidHeaderException
	 *             If the name will not fit in the header.
	 */
	public void writeEntryHeaderMulti(byte[] outbuf, int m)
	throws InvalidHeaderException {

		int offset = 0;

		if (this.tarFormat == UNIX_FORMAT && this.name.length() > 100) {
			throw new InvalidHeaderException(
					"file path is greater than 100 characters, " + this.name);
		}

		offset = TarFileUtil.getFileNameBytes(this.name, outbuf,
				(this.tarFormat == GNU_FORMAT));

		offset = TarFileUtil.getOctalBytes(this.mode, outbuf, offset,
				TarConstants.MODELEN, TarConstants.ZERO_BYTE);

		offset = TarFileUtil.getOctalBytes(this.userId, outbuf, offset,
				TarConstants.UIDLEN, TarConstants.ZERO_BYTE);

		offset = TarFileUtil.getOctalBytes(this.groupId, outbuf, offset,
				TarConstants.GIDLEN, TarConstants.ZERO_BYTE);

		long size = this.size;

		if (logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "The header size in the multi: " + size);
		}

		offset = TarFileUtil.getLongOctalBytes(size, outbuf, offset,
				TarConstants.SIZELEN);

		offset = TarFileUtil.getLongOctalBytes(this.modTime, outbuf, offset,
				TarConstants.MODTIMELEN);

		int csOffset = offset;
		for (int c = 0; c < TarConstants.CHKSUMLEN; c++) {
			outbuf[offset++] = TarConstants.SPACER_BYTE;
		}

		if (m != -1) {
			this.linkFlag = TarConstants.LF_GNUTYPE_MULTIVOL;
		}
		outbuf[offset++] = this.linkFlag;

		offset = TarFileUtil.getNameBytes(this.linkName, outbuf, offset,
				TarConstants.NAMELEN);

		if (this.tarFormat == UNIX_FORMAT) {
			for (int i = 0; i < TarConstants.MAGICLEN; i++) {
				outbuf[offset++] = 0;
			}
		} else {

			offset = TarFileUtil.getNameBytes(this.magic, outbuf, offset,
					TarConstants.MAGICLEN);
			
			if (this.tarFormat == USTAR_FORMAT) {
				outbuf[offset - 2] = TarConstants.ZERO_BYTE;
				outbuf[offset - 1] = TarConstants.ZERO_BYTE;
			}
		}

		offset = TarFileUtil.getNameBytes(this.userName, outbuf, offset,
				TarConstants.UNAMELEN);

		offset = TarFileUtil.getNameBytes(this.groupName, outbuf, offset,
				TarConstants.GNAMELEN);

		offset = TarFileUtil.getOctalBytes(this.devMajor, outbuf, offset,
				TarConstants.DEVLEN, TarConstants.ZERO_BYTE);

		offset = TarFileUtil.getOctalBytes(this.devMinor, outbuf, offset,
				TarConstants.DEVLEN, TarConstants.ZERO_BYTE);

		while (offset < outbuf.length) {
			outbuf[offset++] = 0;
		}

		// this sets the real size if the size is bigger than the 8GB barrier
		if (size > 8589934592l) {
			offset = TarFileUtil.setRealSize(size, outbuf, 124, 12);
		}

		// this sets the offset field for the header
		offset = TarFileUtil.getLongOctalBytesMulti(m, outbuf, 369, 12);

		// this sets the real size of the offset if the size is bigger than the
		// 8GB barrier
		/*
		 * This sets the real size if the size is bigger than the 8GB barrier.
		 * Not supported by USTAR 
		 * (based on http://en.wikipedia.org/wiki/Tar_%28file_format%29#UStar_format)
		 * 
		 * Directory "size" fix contributed by: Bert Becker
		 * <becker@informatik.hu-berlin.de>
		 */
		if(this.tarFormat != USTAR_FORMAT) {
			if (size > 8589934592l) {
				offset = TarFileUtil.setRealSize(size, outbuf, 124, 12);
			}
		}

		this.linkFlag = TarConstants.LF_GNUTYPE_MULTIVOL;

		long checkSum = TarFileUtil.computeCheckSum(outbuf);

		TarFileUtil.getCheckSumOctalBytes(checkSum, outbuf, csOffset,
				TarConstants.CHKSUMLEN);
	}

}
