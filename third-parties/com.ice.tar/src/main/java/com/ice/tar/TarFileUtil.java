package com.ice.tar;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * These are the standard static helpers for parsing/writing the header data.
 * This could probably be cleaned up further.
 * 
 * This library is under the Apache License Version 2.0
 * 
 * Authors:
 * 
 * @author Jeremy Lucier
 * 
 * Thanks to Thomas Ledoux for his many contributions all over this code. 
 * He fixed the byte shifts and discrepancies between this and the official format.
 * 
 */
public class TarFileUtil {

	private static final int BYTE_MASK = 255;


	/**
	 * Parse the checksum octal integer from a header buffer.
	 *
	 * @param value The header value
	 * @param buf The buffer from which to parse.
	 * @param offset The offset into the buffer from which to parse.
	 * @param length The number of header bytes to parse.
	 * @return The integer value of the entry's checksum.
	 */

	public static int getCheckSumOctalBytes(long value, byte[] buf, int offset, int length) {
		getPostfixOctalBytes(value, buf, offset, length, TarConstants.SPACER_BYTE);
		return offset + length;
	}

	/**
	 * Compute the checksum of a tar entry header.
	 *
	 * @param buf The tar entry's header buffer.
	 * @return The computed checksum.
	 */
	public static long computeCheckSum(byte[] buf) {
		long sum = 0;

		for (int i = 0; i < buf.length; ++i) {
			sum += BYTE_MASK & buf[i];
		}

		return sum;
	}




	/**
	 * This method, like getNameBytes(), is intended to place a name into a
	 * TarHeader's buffer. However, this method is sophisticated enough to
	 * recognize long names (name.length() > NAMELEN). In these cases, the
	 * method will break the name into a prefix and suffix and place the name in
	 * the header in 'ustar' format. It is up to the TarEntry to manage the
	 * "entry header format". This method assumes the name is valid for the type
	 * of archive being generated.
	 * 
	 * @param outbuf
	 *            The buffer containing the entry header to modify.
	 * @param newName
	 *            The new name to place into the header buffer.
	 * @return The current offset in the tar header (always
	 *         TarConstants.NAMELEN).
	 * @throws InvalidHeaderException
	 *             If the name will not fit in the header.
	 */
	public static int getFileNameBytes(String newName, byte[] outbuf,
			boolean isGNUTar) throws InvalidHeaderException {

		if (isGNUTar == false && newName.length() > 100) {
			// Locate a pathname "break" prior to the maximum name length...
			int index = newName.indexOf("/", newName.length() - 100);
			if (index == -1) {
				throw new InvalidHeaderException(
						"file name is greater than 100 characters, " + newName);
			}

			// Get the "suffix subpath" of the name.
			String name = newName.substring(index + 1);

			// Get the "prefix subpath", or "prefix", of the name.
			String prefix = newName.substring(0, index);
			if (prefix.length() > TarConstants.PREFIXLEN) {
				throw new InvalidHeaderException(
				"file prefix is greater than 155 characters");
			}

			getNameBytes(name, outbuf, TarConstants.NAMEOFFSET,
					TarConstants.NAMELEN);

			getNameBytes(prefix, outbuf, TarConstants.PREFIXOFFSET,
					TarConstants.PREFIXLEN);

		} else {

			getNameBytes(newName, outbuf, TarConstants.NAMEOFFSET,
					TarConstants.NAMELEN);
		}

		// The offset, regardless of the format, is now the end of the
		// original name field.
		//
		return TarConstants.NAMELEN;
	}

	/**
	 * Parse an octal long integer from a header buffer.
	 * 
	 * @param header
	 *            The header buffer from which to parse.
	 * @param offset
	 *            The offset into the buffer from which to parse.
	 * @param length
	 *            The number of header bytes to parse.
	 * @return The long value of the octal bytes.
	 */
	public static int getLongOctalBytes(long value, byte[] buf, int offset,
			int length) {

		getOctalBytes(value, buf, offset, length, TarConstants.ZERO_BYTE);
		return offset + length;
	}

	/**
	 * Parse an octal long integer from a header buffer this is special for -M
	 * (multi volume drives. Because GNU doesnt accept byte 32 it recuires and
	 * is shifted off one and the regular octal doesnt work.
	 * 
	 *So i call the regular one convert 32 to 48 and then put it in at the
	 * offset
	 * 
	 * @param header
	 *            The header buffer from which to parse.
	 * @param offset
	 *            The offset into the buffer from which to parse.
	 * @param length
	 *            The number of header bytes to parse.
	 * @return The long value of the octal bytes.
	 */
	public static int getLongOctalBytesMulti(long value, byte[] buf, int offset,
			int length) {

		byte[] temp = new byte[length + 1];
		getOctalBytes(value, temp, 0, length + 1, TarConstants.ZERO_BYTE);

		for (int m = 0; m < length + 1; m++) {
			if (temp[m] == 32) {
				temp[m] = (byte) 48;
			}
		}

		for (int i = 0; i < length; i++) {
			if (i > 0) {
				buf[offset + i] = temp[i - 1];
			} else {
				buf[offset + i] = (byte) 48;
			}
		}
		/*
		 * for(int i=0;i<length;i++){
		 * 
		 * if(i>0){ buf[offset+i]=temp[i]; }else if(i==length+2) //break;
		 * buf[offset+i]=(byte) 0; else buf[offset+i]=(byte) 48; }
		 */

		// Free memory
		temp = null;

		return offset + length;
	}

	/**
	 * Move the bytes from the name StringBuffer into the header's buffer.
	 * 
	 * @param header
	 *            The header buffer into which to copy the name.
	 * @param offset
	 *            The offset into the buffer at which to store.
	 * @param length
	 *            The number of header bytes to store.
	 * @return The new offset (offset + length).
	 */
	public static int getNameBytes(String name, byte[] buf, int offset,
			int length) {

		int i = 0;

		int nameLen = name.length();
		for (i = 0; i < length && i < nameLen; ++i) {
			buf[offset + i] = (byte) name.charAt(i);
		}

		// Leave as a prefix for loop...
		for (; i < length; ++i) {
			buf[offset + i] = 0;
		}

		return offset + length;
	}

	/**
	 * Parse an octal integer from a header buffer.
	 * 
	 * @param header
	 *            The header buffer from which to parse.
	 * @param offset
	 *            The offset into the buffer from which to parse.
	 * @param length
	 *            The number of header bytes to parse.
	 * @return The integer value of the octal bytes.
	 */
	public static int getOctalBytes(long value, byte[] buf, int offset, int length, byte prefix) {

		// Leave the prefix calls
		int idx = length - 1;

		// Set the last byte null
		buf[offset + idx] = 0;
		--idx;

		if (value == 0) {
			// Set the last value to zero
			buf[offset + idx] = TarConstants.ZERO_BYTE;
			--idx;
		} else {
			for (long val = value; idx >= 0 && val > 0; --idx) {
				buf[offset + idx] = (byte) (TarConstants.ZERO_BYTE + (byte) (val & 7));
				val = val >> 3;
			}
		}

		// Leave for loop a a prefix iterator
		for (; idx >= 0; --idx) {
			buf[offset + idx] = prefix; // Was a spacer byte
		}

		return offset + length;
	}

	/**
	 * Parse an octal integer from a header buffer. This is postfixed, required for checksum
	 * to match tar in linux's behavior
	 * 
	 * @param header
	 *            The header buffer from which to parse.
	 * @param offset
	 *            The offset into the buffer from which to parse.
	 * @param length
	 *            The number of header bytes to parse.
	 * @return The integer value of the octal bytes.
	 */
	public static int getPostfixOctalBytes(long value, byte[] buf, int offset, int length, byte postfix) {

		int leftIdx = 0;
		if(value == 0) {
			buf[offset + leftIdx] = TarConstants.ZERO_BYTE;
			leftIdx++;
		} else {

			// We're going to shove all the digits of the long into a byte ArrayList,
			// then we're going to put them back in rev order into the buffer. 
			// This isn't efficient and should probably be rewritten when I find some
			// time.

			//TODO: Optimize
			ArrayList<Byte> vals = new ArrayList<Byte>();
			long val = value;
			while(val > 0) {
				vals.add((byte) (TarConstants.ZERO_BYTE + (byte) (val & 7)));
				val = val >> 3;
			}

			// Now let's iterate in reverse through it and put it on the buffer
			for(int x = vals.size() - 1; x >= 0; x--) {
				buf[offset + leftIdx] = vals.get(x);
				leftIdx++;
			}
			
			vals.clear();
		}


		// NUL terminate after sequence
		buf[offset + leftIdx] = 0;
		leftIdx++;

		// Postfix iterator
		while(leftIdx < length) {
			buf[offset + leftIdx] = postfix; // Was a spacer byte
			leftIdx++;
		}

		return offset + length;
	}

	/**
	 * Move the bytes from the offset StringBuffer into the header's buffer.
	 * 
	 * @param header
	 *            The header buffer into which to copy the name.
	 * @param offset
	 *            The offset into the buffer at which to store.
	 * @param length
	 *            The number of header bytes to store.
	 * @return The new offset (offset + length).
	 */
	public static int getOffBytes(StringBuffer offVal, byte[] buf, int offset,
			int length) {
		int i;

		for (i = 0; i < length && i < offVal.length(); ++i) {
			buf[offset + i] = (byte) offVal.charAt(i);
		}

		// Leave prefix loop alone...
		for (; i < length; ++i) {
			buf[offset + i] = 0;
		}

		return offset + length;
	}

	/**
	 * gets the real size as binary data for files that are larger than 8GB
	 */
	public static long getSize(byte[] header, int offset, int length) {

		long test = parseOctal(header, offset, length);
		if (test <= 0 && header[offset] == (byte) 128) {

			byte[] last = new byte[length];

			for (int i = 0; i < length; i++) {
				last[i] = header[offset + i];
			}
			last[0] = (byte) 0;

			long rSize = new BigInteger(last).longValue();

			// Free memory
			last = null;

			return rSize;

		}
		return test;
	}

	/**
	 * Parse a file name from a header buffer. This is different from
	 * parseName() in that is recognizes 'ustar' names and will handle adding on
	 * the "prefix" field to the name.
	 * 
	 * Contributed by Dmitri Tikhonov <dxt2431@yahoo.com>
	 * 
	 * @param header
	 *            The header buffer from which to parse.
	 * @param offset
	 *            The offset into the buffer from which to parse.
	 * @param length
	 *            The number of header bytes to parse.
	 * @return The header's entry name.
	 */
	public static String parseFileName(byte[] header) {

		StringBuilder result = new StringBuilder(256);

		// If header[345] is not equal to zero, then it is the "prefix"
		// that 'ustar' defines. It must be prepended to the "normal"
		// name field. We are responsible for the separating '/'.
		//
		if (header[345] != 0) {
			for (int i = 345; i < 500 && header[i] != 0; ++i) {
				result.append((char) header[i]);
			}

			result.append("/");
		}

		for (int i = 0; i < 100 && header[i] != 0; ++i) {
			result.append((char) header[i]);
		}

		return result.toString();
	}

	/**
	 * Parse an entry name from a header buffer.
	 * 
	 * @param header
	 *            The header buffer from which to parse.
	 * @param offset
	 *            The offset into the buffer from which to parse.
	 * @param length
	 *            The number of header bytes to parse.
	 * @return The header's entry name.
	 */
	public static String parseName(byte[] header, int offset, int length) {
		StringBuilder result = new StringBuilder(length);

		int end = offset + length;
		for (int i = offset; i < end; ++i) {
			if (header[i] == 0) {
				break;
			}
			result.append((char) header[i]);
		}

		return result.toString();
	}

	/**
	 * Parse an octal string from a header buffer. This is used for the file
	 * permission mode value.
	 * 
	 * @param header
	 *            The header buffer from which to parse.
	 * @param offset
	 *            The offset into the buffer from which to parse.
	 * @param length
	 *            The number of header bytes to parse.
	 * @return The long value of the octal string.
	 */
	public static long parseOctal(byte[] header, int offset, int length) {

		long result = 0;
		boolean stillPadding = true;

		int end = offset + length;
		for (int i = offset; i < end; ++i) {
			if (header[i] == 0) {
				break;
			}

			if (header[i] == TarConstants.SPACER_BYTE
					|| header[i] == TarConstants.ZERO_BYTE) { // == '0'
				if (stillPadding) {
					continue;
				}

				if (header[i] == TarConstants.SPACER_BYTE) {
					break;
				}
			}

			stillPadding = false;

			result = (result << 3) + (header[i] - TarConstants.ZERO_BYTE); // -
			// '0'
		}

		return result;
	}

	/**
	 * sets the real size as binary data for files that are larger than 8GB
	 */
	public static int setRealSize(long value, byte[] buf, int offset, int length) {

		BigInteger Rsize = new BigInteger("" + value);
		byte[] last = new byte[12];
		byte[] copier = Rsize.toByteArray();
		for (int i = 0; i < copier.length; i++) {
			last[last.length - copier.length + i] = copier[i];
		}
		last[0] = (byte) 128;

		/*
		 * for(int i=0;i<last.length;i++){ //System.out.print(i+":");
		 * //printBinary(last[i]); }
		 */

		int lastLen = last.length;
		for (int i = 0; i < lastLen; i++) {
			buf[offset + i] = last[i];
		}

		last = null;
		copier = null;

		return offset + length;
	}
}
