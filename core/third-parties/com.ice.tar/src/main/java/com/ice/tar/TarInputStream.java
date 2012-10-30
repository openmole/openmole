package com.ice.tar;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The TarInputStream reads a UNIX/Gnutar/Ustar tar archive as an InputStream.
 * Methods are provided to position at each successive entry in the archive, and
 * the read each entry as a normal input stream using read().
 * 
 * This library is under the Apache License Version 2.0
 * 
 * Authors:
 * 
 * @author Jeremy Lucier
 * @author Timothy Gerard Endres (Original Author)
 * 
 */

public class TarInputStream extends FilterInputStream {

	private static final Logger logger = Logger.getLogger(TarInputStream.class.getName());
    private static final int BYTE_MASK = 0xFF;
	
	private boolean hasHitEOF;

	/*
	 * Kerry Menzel <kmenzel@cfl.rr.com> Contributed the code to support file
	 * sizes greater than 2GB (longs versus ints).
	 */
	private long entrySize;
	private long entryOffset;

	private byte[] oneBuf;
	private byte[] readBuf;

	private TarBuffer buffer;
	private TarEntry currEntry;

	public TarInputStream(InputStream is) {
		this(is, TarConstants.DEFAULT_BLKSIZE, TarConstants.DEFAULT_RCDSIZE);
	}

	public TarInputStream(InputStream is, int blockSize) {
		this(is, blockSize, TarConstants.DEFAULT_RCDSIZE);
	}

	public TarInputStream(InputStream is, int blockSize, int recordSize) {

		super(is);

		this.buffer = new TarBuffer(is, blockSize, recordSize);

		this.readBuf = null;
		this.oneBuf = new byte[1];
		this.hasHitEOF = false;
	}

	/**
	 * Get the available data that can be read from the current entry in the
	 * archive. This does not indicate how much data is left in the entire
	 * archive, only in the current entry. This value is determined from the
	 * entry's size header field and the amount of data already read from the
	 * current entry.
	 * 
	 * 
	 * @return The number of available bytes for the current entry.
	 */
	@Override
	public int available() throws IOException {
		if (this.entrySize - this.entryOffset > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) (this.entrySize - this.entryOffset);
	}

	/**
	 * Closes this stream. Calls the TarBuffer's close() method.
	 */
	@Override
	public void close() throws IOException {

		// Remove any remaining TarEntry's
		this.currEntry = null;

		// Close the reading buffer
		this.buffer.close();
	}

	/**
	 * Copies the contents of the current tar archive entry directly into an
	 * output stream.
	 * 
	 * @param out
	 *            The OutputStream into which to write the entry's data.
	 */
	public void copyEntryContents(OutputStream out) throws IOException {
		byte[] buf = new byte[TarConstants.LARGE_BUFFER_SIZE];

		int numRead = -1;
		while ((numRead = this.read(buf, 0, TarConstants.LARGE_BUFFER_SIZE)) > -1) {

			out.write(buf, 0, numRead);
		}
		out.flush();

		buf = null;
	}

	/**
	 * Get the number of bytes into the current TarEntry. This method returns
	 * the number of bytes that have been read from the current TarEntry's data.
	 * 
	 * @returns The current entry offset.
	 */

	public long getEntryPosition() {
		return this.entryOffset;
	}

	/**
	 * Get the next entry in this tar archive. This will skip over any remaining
	 * data in the current entry, if there is one, and place the input stream at
	 * the header of the next entry, and read the header and instantiate a new
	 * TarEntry from the header bytes and return that entry. If there are no
	 * more entries in the archive, null will be returned to indicate that the
	 * end of the archive has been reached.
	 * 
	 * @return The next TarEntry in the archive, or null.
	 */

	public TarEntry getNextEntry() throws IOException {

		if (this.hasHitEOF) {
			return null;
		}

		if (this.currEntry != null) {

			long numToSkip = this.entrySize - this.entryOffset;

			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, "TarInputStream: SKIP currENTRY '"
						+ this.currEntry.getName() + "' SZ " + this.entrySize
						+ " OFF " + this.entryOffset + "  skipping "
						+ numToSkip + " bytes");
			}

			if (numToSkip > 0) {
				this.skip(numToSkip);
			}

			// Starting a new file, free up resources
			this.currEntry = null;
			this.readBuf = null;
		}

		byte[] headerBuf = this.buffer.readRecord();

		if (headerBuf == null) {
			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, "READ NULL RECORD");
			}

			this.hasHitEOF = true;
		} else if (this.buffer.isEOFRecord(headerBuf)) {

			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, "READ EOF RECORD");
			}

			this.hasHitEOF = true;
		}

		if (this.hasHitEOF) {
			this.currEntry = null;
		} else {

			try {

				// Create a new TarEntry
				this.currEntry = new TarEntry(headerBuf);

				if (logger.isLoggable(Level.FINEST)) {
					logger.log(Level.FINEST, "TarInputStream: SET CURR ENTRY '"
							+ this.currEntry.getName() + "' size = "
							+ this.currEntry.getSize());
				}

				this.entryOffset = 0;
				this.entrySize = this.currEntry.getSize();

			} catch (InvalidHeaderException ex) {

				this.entrySize = 0;
				this.entryOffset = 0;
				this.currEntry = null;

				throw new InvalidHeaderException("bad header in block "
						+ this.buffer.getCurrentBlockNum() + " record "
						+ this.buffer.getCurrentRecordNum() + ", "
						+ ex.getMessage());
			}
		}

		// JRL - Gnutar longlink support!
		if (currEntry != null && currEntry.isGNULongNameEntry()) {

			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, "LongLink File Detected");
			}

			// Read in the name
			StringBuffer longName = new StringBuffer();
			byte[] buf = new byte[TarConstants.SMALL_BUFFER_SIZE];
			int length = 0;
			while ((length = read(buf)) >= 0) {
				longName.append(new String(buf, 0, length));
			}

			// Free memory
			buf = null;

			getNextEntry();

			if (currEntry == null) {
				// Malformed tar file - long entry name not followed by entry
				return null;
			}

			// Remove trailing null terminator
			int longNameLen = longName.length();
			if (longNameLen > 0 && longName.charAt(longNameLen - 1) == 0) {

				longName.deleteCharAt(longNameLen - 1);
			}
			currEntry.setName(longName.toString());

			// Free up memory
			longName.setLength(0);
			longName = null;
		}

		return this.currEntry;
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
	 * Get the number of bytes into the stream we are currently at. This method
	 * accounts for the blocking stream that tar uses, so it represents the
	 * actual position in input stream, as opposed to the place where the tar
	 * archive parsing is.
	 * 
	 * @returns The current file pointer.
	 */

	public long getStreamPosition() {
		return buffer.getBlockSize() * buffer.getCurrentBlockNum()
		+ buffer.getCurrentRecordNum();
	}

	/**
	 * Since we do not support marking just yet, we do nothing.
	 * 
	 * @param markLimit
	 *            The limit to mark.
	 */
	@Override
	public void mark(int markLimit) {
	}

	/**
	 * Since we do not support marking just yet, we return false.
	 * 
	 * @return False.
	 */
	@Override
	public boolean markSupported() {
		return false;
	}

	/**
	 * Reads a byte from the current tar archive entry.
	 * 
	 * This method simply calls read( byte[], int, int ).
	 * 
	 * @return The byte read, or -1 at EOF.
	 */
	@Override
	public int read() throws IOException {
		int num = read(oneBuf, 0, 1);
		return num == -1 ? -1 : ((int) oneBuf[0]) & BYTE_MASK;
	}

	/**
	 * Reads bytes from the current tar archive entry.
	 * 
	 * This method simply calls read( byte[], int, int ).
	 * 
	 * @param buf
	 *            The buffer into which to place bytes read.
	 * @return The number of bytes read, or -1 at EOF.
	 */
	@Override
	public int read(byte[] buf) throws IOException {
		return this.read(buf, 0, buf.length);
	}

	/**
	 * Reads bytes from the current tar archive entry.
	 * 
	 * This method is aware of the boundaries of the current entry in the
	 * archive and will deal with them as if they were this stream's start and
	 * EOF.
	 * 
	 * @param buf
	 *            The buffer into which to place bytes read.
	 * @param offset
	 *            The offset at which to place bytes read.
	 * @param numToRead
	 *            The number of bytes to read.
	 * @return The number of bytes read, or -1 at EOF.
	 */
	@Override
	public int read(byte[] buf, int offset, int numToRead) throws IOException {
		int totalRead = 0;

		if (entryOffset >= entrySize) {
			return -1;
		}

		if ((numToRead + entryOffset) > entrySize) {
			numToRead = (int) (entrySize - entryOffset);
		}

		if (readBuf != null) {
			int sz = (numToRead > readBuf.length) ? readBuf.length
					: numToRead;

			System.arraycopy(readBuf, 0, buf, offset, sz);

			if (sz >= readBuf.length) {
				readBuf = null;
			} else {
				int newLen = readBuf.length - sz;
				byte[] newBuf = new byte[newLen];

				System.arraycopy(readBuf, sz, newBuf, 0, newLen);

				readBuf = newBuf;
			}

			totalRead += sz;
			numToRead -= sz;
			offset += sz;
		}

		while (numToRead > 0) {
			byte[] rec = buffer.readRecord();

			if (rec == null) {
				// Unexpected EOF!
				throw new IOException("unexpected EOF with " + numToRead
						+ " bytes unread");
			}

			int sz = numToRead;
			int recLen = rec.length;

			if (recLen > sz) {
				System.arraycopy(rec, 0, buf, offset, sz);

				readBuf = new byte[recLen - sz];

				System.arraycopy(rec, sz, readBuf, 0, recLen - sz);
			} else {
				sz = recLen;

				System.arraycopy(rec, 0, buf, offset, recLen);
			}

			totalRead += sz;
			numToRead -= sz;
			offset += sz;
		}

		entryOffset += totalRead;

		return totalRead;
	}

	/**
	 * Since we do not support marking just yet, we do nothing.
	 */
	@Override
	public void reset() {
	}

	/**
	 * Skip bytes in the input buffer. This skips bytes in the current entry's
	 * data, not the entire archive, and will stop at the end of the current
	 * entry's data if the number to skip extends beyond that point.
	 * 
	 * @param numToSkip
	 *            The number of bytes to skip.
	 * @return The actual number of bytes skipped.
	 */
	@Override
	public long skip(long numToSkip) throws IOException {

		// TODO: Review and possibly revise
		// This is horribly inefficient, but it ensures that we
		// properly skip over bytes via the TarBuffer...
		//
		byte[] skipBuf = new byte[TarConstants.SKIP_BUFFER_SIZE];
		long skip = numToSkip;
		int realSkip = -1;
		int numRead = -1;

		while (skip > 0) {
			realSkip = skip > skipBuf.length ? skipBuf.length : (int) skip;
			numRead = read(skipBuf, 0, realSkip);
			if (numRead == -1) {
				break;
			}
			skip -= numRead;
		}

		// Free memory
		skipBuf = null;

		return numToSkip - skip;

	}

}
