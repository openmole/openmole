package com.ice.tar;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The TarOutputStream writes a UNIX tar archive as an OutputStream. Methods are
 * provided to put entries, and then write their contents by writing to this
 * stream using write().
 * 
 * 
 * This library is under the Apache License Version 2.0
 * 
 * @author Jeremy Lucier
 * @author Timothy Gerard Endres (Original Author)
 * 
 */

public class TarOutputStream extends FilterOutputStream {

	private static final Logger logger = Logger.getLogger(TarOutputStream.class.getName());

	/*
	 * Kerry Menzel <kmenzel@cfl.rr.com> Contributed the code to support file
	 * sizes greater than 2GB (longs versus ints).
	 */

	private long currSize;
	private long currBytes;
	private byte[] oneBuf;
	private byte[] recordBuf;
	private int assemLen;
	private byte[] assemBuf;
	private TarBuffer buffer;

	public TarOutputStream(OutputStream os) {
		this(os, TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE);
	}

	public TarOutputStream(OutputStream os, int blockSize) {
		this(os, blockSize, TarConstants.DEFAULT_RCDSIZE);
	}

	public TarOutputStream(OutputStream os, int blockSize, int recordSize) {
		super(os);

		this.buffer = new TarBuffer(os, blockSize, recordSize);

		this.assemLen = 0;
		this.assemBuf = new byte[recordSize];
		this.recordBuf = new byte[recordSize];
		this.oneBuf = new byte[1];
	}

	/**
	 * Ends the TAR archive and closes the underlying OutputStream. This means
	 * that finish() is called followed by calling the TarBuffer's close().
	 */

	@Override
	public void close() throws IOException {
		this.finish();
		this.buffer.close();
	}

	/**
	 * Ends the TAR archive and closes the underlying OutputStream. This means
	 * that finish() is called followed by calling the TarBuffer's close().
	 */

	public void closeMulti() throws IOException {
		this.flush();
		this.buffer.close();
	}

	/**
	 * Close an entry. This method MUST be called for all file entries that
	 * contain data. The reason is that we must buffer data written to the
	 * stream in order to satisfy the buffer's record based writes. Thus, there
	 * may be data fragments still being assembled that must be written to the
	 * output stream before this entry is closed and the next entry written.
	 */
	public void closeEntry() throws IOException {

		if (this.assemLen > 0) {
			for (int i = this.assemLen; i < this.assemBuf.length; ++i) {
				this.assemBuf[i] = 0;
			}

			this.buffer.writeRecord(this.assemBuf);

			this.currBytes += this.assemLen;

			this.assemLen = 0;
		}

		if (this.currBytes < this.currSize) {
			throw new IOException("entry closed at '" + this.currBytes
					+ "' before the '" + this.currSize
					+ "' bytes specified in the header were written");
		}
	}

	/**
	 * Close an entry. This method MUST be called for all file entries that
	 * contain data. The reason is that we must buffer data written to the
	 * stream in order to satisfy the buffer's record based writes. Thus, there
	 * may be data fragments still being assembled that must be written to the
	 * output stream before this entry is closed and the next entry written.
	 * 
	 * this is the version that should be called when spanning media with the -M
	 * option that is now avialable
	 */
	public void closeEntryMulti() throws IOException {
		if (this.assemLen > 0) {
			this.buffer.writeRecord(this.assemBuf);
			this.currBytes += this.assemLen;
			this.assemLen = 0;
		}

	}

	/**
	 * Ends the TAR archive without closing the underlying OutputStream. The
	 * result is that the EOF record of nulls is written.
	 */

	public void finish() throws IOException {
		this.writeEOFRecord();
	}

	/**
	 * Get the record size being used by this stream's TarBuffer.
	 * 
	 * @return The TarBuffer record size.
	 */
	public int getRecordSize() {
		return this.buffer.getRecordSize();
	}

	/**
	 * Put an entry on the output stream. This writes the entry's header record
	 * and positions the output stream for writing the contents of the entry.
	 * Once this method is called, the stream is ready for calls to write() to
	 * write the entry's contents. Once the contents are written, closeEntry()
	 * <B>MUST</B> be called to ensure that all buffered data is completely
	 * written to the output stream.
	 * 
	 * @param entry
	 *            The TarEntry to be written to the archive.
	 */
	public void putNextEntry(TarEntry entry) throws IOException {

		String name = entry.getName();

		// NOTE
		// This check is not adequate, because the maximum file length that
		// can be placed into a POSIX (ustar) header depends on the precise
		// locations of the path elements (slashes) within the file's full
		// pathname. For this reason, writeEntryHeader() can still throw an
		// InvalidHeaderException if the file's full pathname will not fit
		// in the header.

		if (entry.getName().length() >= TarConstants.NAMELEN
				|| entry.getTarFormat() != TarEntry.UNIX_FORMAT
				&& name.length() > TarConstants.NAMELEN
						+ TarConstants.PREFIXLEN) {

			// JRL - Gnutar LongLink support (if the entry is gnutar, put the
			// filename as a file and branch it)
			if (entry.getTarFormat() == TarEntry.GNU_FORMAT) {

				// create a TarEntry for the LongLink, the contents
				// of which are the entry's name
				TarEntry longLinkEntry = new TarEntry(
						TarConstants.GNU_LONGLINK,
						TarConstants.LF_GNUTYPE_LONGNAME);

                                longLinkEntry.setModTime(entry.getModTime());
				longLinkEntry.setSize(entry.getName().length() + 1);
				putNextEntry(longLinkEntry);
				write(entry.getName().getBytes());
				write(0);
				closeEntry();

				longLinkEntry = null;

			} else {
				throw new InvalidHeaderException(
						"file name '"
								+ name
								+ "' is too long ( "
								+ name.length()
								+ " > "
								+ (entry.getTarFormat() == TarEntry.UNIX_FORMAT ? TarConstants.NAMELEN
										: TarConstants.NAMELEN
												+ TarConstants.PREFIXLEN)
								+ " bytes )");
			}
		}

		entry.writeEntryHeader(this.recordBuf);
		this.buffer.writeRecord(this.recordBuf);

		this.currBytes = 0;

		if (entry.isDirectory()) {
			this.currSize = 0;
		} else {
			this.currSize = entry.getSize();
		}
	}

	/**
	 * Put an entry on the output stream. This writes the entry's header record
	 * and positions the output stream for writing the contents of the entry.
	 * Once this method is called, the stream is ready for calls to write() to
	 * write the entry's contents. Once the contents are written, closeEntry()
	 * <B>MUST</B> be called to ensure that all buffered data is completely
	 * written to the output stream.
	 * 
	 *this does a header for a multipart -M spanning archive length is the size
	 * for the header field atByte is the offset field
	 * 
	 * @param entry
	 *            The TarEntry to be written to the archive.
	 */
	public void putNextEntry(TarEntry entry, long atByte, long length)
			throws IOException {

		String name = entry.getName();

		// NOTE
		// This check is not adequate, because the maximum file length that
		// can be placed into a POSIX (ustar) header depends on the precise
		// locations of the path elements (slashes) within the file's full
		// pathname. For this reason, writeEntryHeader() can still throw an
		// InvalidHeaderException if the file's full pathname will not fit
		// in the header.

		if (entry.getName().length() >= TarConstants.NAMELEN
				|| entry.getTarFormat() != TarEntry.UNIX_FORMAT
				&& name.length() > TarConstants.NAMELEN
						+ TarConstants.PREFIXLEN) {

			// JRL - Gnutar LongLink support (if the entry is gnutar, put the
			// filename as a file and branch it)
			if (entry.getTarFormat() == TarEntry.GNU_FORMAT) {

				// create a TarEntry for the LongLink, the contents
				// of which are the entry's name
				TarEntry longLinkEntry = new TarEntry(
						TarConstants.GNU_LONGLINK,
						TarConstants.LF_GNUTYPE_LONGNAME);
                                
                                longLinkEntry.setModTime(entry.getModTime());

				longLinkEntry.setSize(entry.getName().length() + 1);
				putNextEntry(longLinkEntry);
				write(entry.getName().getBytes());
				write(0);
				closeEntry();

			} else {
				throw new InvalidHeaderException(
						"file name '"
								+ name
								+ "' is too long ( "
								+ name.length()
								+ " > "
								+ (entry.getTarFormat() == TarEntry.UNIX_FORMAT ? TarConstants.NAMELEN
										: TarConstants.NAMELEN
												+ TarConstants.PREFIXLEN)
								+ " bytes )");
			}
		}
		if (length != -1) {
			entry.setSize(new Long(length).longValue());
		}
		entry.writeEntryHeaderMulti(this.recordBuf, new Long(atByte)
						.intValue());

		this.buffer.writeRecord(this.recordBuf);

		this.currBytes = 0;

		if (entry.isDirectory()) {
			this.currSize = 0;
		} else {
			this.currSize = entry.getSize();
		}
	}

	/**
	 * Writes bytes to the current tar archive entry.
	 * 
	 * This method simply calls read( byte[], int, int ).
	 * 
	 * @param wBuf
	 *            The buffer to write to the archive.
	 * @return The number of bytes read, or -1 at EOF.
	 */
	@Override
	public void write(byte[] wBuf) throws IOException {
		this.write(wBuf, 0, wBuf.length);
	}

	/**
	 * Writes bytes to the current tar archive entry. This method is aware of
	 * the current entry and will throw an exception if you attempt to write
	 * bytes past the length specified for the current entry. The method is also
	 * (painfully) aware of the record buffering required by TarBuffer, and
	 * manages buffers that are not a multiple of recordsize in length,
	 * including assembling records from small buffers.
	 * 
	 * This method simply calls read( byte[], int, int ).
	 * 
	 * @param wBuf
	 *            The buffer to write to the archive.
	 * @param wOffset
	 *            The offset in the buffer from which to get bytes.
	 * @param numToWrite
	 *            The number of bytes to write.
	 */
	@Override
	public void write(byte[] wBuf, int wOffset, int numToWrite)
			throws IOException {
		if (this.currBytes != 0) {
			if (this.currBytes + numToWrite > this.currSize) {
				throw new IOException("request to write '" + numToWrite
						+ "' bytes exceeds size in header of '" + this.currSize
						+ "' bytes");
			}
		}

		//
		// We have to deal with assembly!!!
		// The programmer can be writing little 32 byte chunks for all
		// we know, and we must assemble complete records for writing.
		// REVIEW Maybe this should be in TarBuffer? Could that help to
		// eliminate some of the buffer copying.
		//
		if (this.assemLen > 0) {
			if (this.assemLen + numToWrite >= this.recordBuf.length) {
				int aLen = this.recordBuf.length - this.assemLen;

				System.arraycopy(this.assemBuf, 0, this.recordBuf, 0,
						this.assemLen);

				System.arraycopy(wBuf, wOffset, this.recordBuf, this.assemLen,
						aLen);

				this.buffer.writeRecord(this.recordBuf);

				this.currBytes += this.recordBuf.length;

				wOffset += aLen;
				numToWrite -= aLen;
				this.assemLen = 0;
			} else { // ( (this.assemLen + numToWrite ) < this.recordBuf.length
						// )

				System.arraycopy(wBuf, wOffset, this.assemBuf, this.assemLen,
						numToWrite);
				wOffset += numToWrite;
				this.assemLen += numToWrite;
				numToWrite -= numToWrite;
			}
		}

		//
		// When we get here we have EITHER:
		// o An empty "assemble" buffer.
		// o No bytes to write (numToWrite == 0)
		//

		while (numToWrite > 0) {
			if (numToWrite < this.recordBuf.length) {
				System.arraycopy(wBuf, wOffset, this.assemBuf, this.assemLen,
						numToWrite);
				this.assemLen += numToWrite;
				break;
			}

			this.buffer.writeRecord(wBuf, wOffset);

			long num = this.recordBuf.length;
			this.currBytes += num;
			numToWrite -= num;
			wOffset += num;
		}
	}

	/**
	 * Writes a byte to the current tar archive entry.
	 * 
	 * This method simply calls read( byte[], int, int ).
	 * 
	 * @param b
	 *            The byte written.
	 */
	@Override
	public void write(int b) throws IOException {
		this.oneBuf[0] = (byte) b;
		this.write(this.oneBuf, 0, 1);
	}

	/**
	 * Write an EOF (end of archive) record to the tar archive. An EOF record
	 * consists of a record of all zeros.
	 */
	private void writeEOFRecord() throws IOException {

		if (logger.isLoggable(Level.FINEST)) {
			logger
					.log(Level.FINEST,
							"********************Writting end of file *************************");
		}
		for (int i = 0; i < this.recordBuf.length; ++i) {
			this.recordBuf[i] = 0;
		}
		this.buffer.writeRecord(this.recordBuf);
	}

}
