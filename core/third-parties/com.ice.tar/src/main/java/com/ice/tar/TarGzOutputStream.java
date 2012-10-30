package com.ice.tar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Outputs tar.gz files. Added functionality that it doesn't need to know the
 * size of an entry. If an entry has zero size when it is put in the Tar, then
 * it buffers it until it's closed and it knows the size.
 * 
 * This library is under the Apache License Version 2.0
 * 
 * Authors:
 * 
 * @author Jeremy Lucier
 * @author "Bay" <bayard@generationjava.com> (Original Author)
 * 
 */

public class TarGzOutputStream extends TarOutputStream {

	private TarOutputStream tos = null;
	private GZIPOutputStream gzip = null;
	private ByteArrayOutputStream bos = null;
	private TarEntry currentEntry = null;

	public TarGzOutputStream(OutputStream out) throws IOException {
		super(null);
		this.gzip = new GZIPOutputStream(out);
		this.tos = new TarOutputStream(this.gzip);
		this.bos = new ByteArrayOutputStream();
	}

	// proxy all methods, but buffer if unknown size

	@Override
	public void close() throws IOException {
		this.tos.close();
		this.gzip.finish();
	}

	@Override
	public void closeEntry() throws IOException {
		if (this.currentEntry == null) {
			this.tos.closeEntry();
		} else {
			this.currentEntry.setSize(bos.size());
			this.tos.putNextEntry(this.currentEntry);
			this.bos.writeTo(this.tos);
			this.tos.closeEntry();
			this.currentEntry = null;
			this.bos = new ByteArrayOutputStream();
		}
	}

	@Override
	public void finish() throws IOException {
		if (this.currentEntry != null) {
			closeEntry();
		}

		this.tos.finish();
	}

	@Override
	public int getRecordSize() {
		return this.tos.getRecordSize();
	}

	@Override
	public void putNextEntry(TarEntry entry) throws IOException {
		if (entry.getSize() != 0) {
			this.tos.putNextEntry(entry);
		} else {
			this.currentEntry = entry;
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		if (this.currentEntry == null) {
			this.tos.write(b);
		} else {
			this.bos.write(b);
		}
	}

	@Override
	public void write(byte[] b, int start, int length) throws IOException {
		if (this.currentEntry == null) {
			this.tos.write(b, start, length);
		} else {
			this.bos.write(b, start, length);
		}
	}

	@Override
	public void write(int b) throws IOException {
		if (this.currentEntry == null) {
			this.tos.write(b);
		} else {
			this.bos.write(b);
		}
	}

}
