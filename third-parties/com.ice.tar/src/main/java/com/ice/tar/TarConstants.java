package com.ice.tar;

/**
 * Contains all of the constants we use for the tar files reading and writing.
 * 
 * This library is under the Apache License Version 2.0
 * 
 * Authors:
 * 
 * @author Jeremy Lucier
 * 
 */
public class TarConstants {

	/**
	 * The length of the name field in a header buffer.
	 */
	public static final int NAMELEN = 100;
	/**
	 * The offset of the name field in a header buffer.
	 */
	public static final int NAMEOFFSET = 0;
	/**
	 * The length of the name prefix field in a header buffer.
	 */
	public static final int PREFIXLEN = 155;
	/**
	 * The offset of the name prefix field in a header buffer.
	 */
	public static final int PREFIXOFFSET = 345;
	/**
	 * The length of the mode field in a header buffer.
	 */
	public static final int MODELEN = 8;
	/**
	 * The length of the user id field in a header buffer.
	 */
	public static final int UIDLEN = 8;
	/**
	 * The length of the group id field in a header buffer.
	 */
	public static final int GIDLEN = 8;
	/**
	 * The length of the checksum field in a header buffer.
	 */
	public static final int CHKSUMLEN = 8;
	/**
	 * The length of the size field in a header buffer.
	 */
	public static final int SIZELEN = 12;
	/**
	 * The length of the magic field in a header buffer.
	 */
	public static final int MAGICLEN = 8;
	/**
	 * The length of the modification time field in a header buffer.
	 */
	public static final int MODTIMELEN = 12;
	/**
	 * The length of the user name field in a header buffer.
	 */
	public static final int UNAMELEN = 32;
	/**
	 * The length of the group name field in a header buffer.
	 */
	public static final int GNAMELEN = 32;
	/**
	 * The length of the devices field in a header buffer.
	 */
	public static final int DEVLEN = 8;

	/**
	 * LF_ constants represent the "link flag" of an entry, or more commonly,
	 * the "entry type". This is the "old way" of indicating a normal file.
	 */
	public static final byte LF_OLDNORM = 0;
	/**
	 * Normal file type.
	 */
	public static final byte LF_NORMAL = (byte) '0';
	/**
	 * Link file type.
	 */
	public static final byte LF_LINK = (byte) '1';
	/**
	 * Symbolic link file type.
	 */
	public static final byte LF_SYMLINK = (byte) '2';
	/**
	 * Character device file type.
	 */
	public static final byte LF_CHR = (byte) '3';
	/**
	 * Block device file type.
	 */
	public static final byte LF_BLK = (byte) '4';
	/**
	 * Directory file type.
	 */
	public static final byte LF_DIR = (byte) '5';
	/**
	 * FIFO (pipe) file type.
	 */
	public static final byte LF_FIFO = (byte) '6';
	/**
	 * Contiguous file type.
	 */
	public static final byte LF_CONTIG = (byte) '7';

	
	/**
	 * Identifies the *next* file on the tape as having a long name.
	 */
	public static final byte LF_GNUTYPE_LONGNAME = (byte) 'L';
	
	/**
	 * Identifies a multi-volume gnutar file
	 */
	public static final byte LF_GNUTYPE_MULTIVOL = (byte) 'M';
	
	/**
	 * The magic tag representing a POSIX tar archive.
	 */
	public static final String TMAGIC = "ustar";

	/** OLDGNU_MAGIC uses both magic and version fields, which are contiguous.
	   Found in an archive, it indicates an old GNU header format, which will be
	   hopefully become obsolete.  With OLDGNU_MAGIC, uname and gname are
	   valid, though the header is not truly POSIX conforming.  */
	public static final String OLDGNU_TMAGIC = "ustar  "; 

	/**
	 * The name of the GNU tar entry which contains a long name.
	 */
	public static final String GNU_LONGLINK = "././@LongLink";

	
	/**
	 * Default RCD Size.
	 */
	public static final int DEFAULT_RCDSIZE = 512;

	/**
	 * Default block size.
	 */
	public static final int DEFAULT_BLKSIZE = DEFAULT_RCDSIZE * 20;

	/** Small Buffer Size */
	public static final int SMALL_BUFFER_SIZE = 256;

	/** Skip Buffer Size */
	public static final int SKIP_BUFFER_SIZE = 8192;

	/** Large Buffer Size */
	public static final int LARGE_BUFFER_SIZE = 32768;

	
	
	/** We cast this constantly **/
	public static final byte SPACER_BYTE = (byte) ' ';

	/** We cast this constantly **/
	public static final byte ZERO_BYTE = (byte) '0';

}
