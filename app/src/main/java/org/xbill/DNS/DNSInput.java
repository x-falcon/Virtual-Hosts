// Copyright (c) 2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.nio.ByteBuffer;

/**
 * An class for parsing DNS messages.
 *
 * @author Brian Wellington
 */

public class DNSInput {

private ByteBuffer byteBuffer;
private int saved_pos;
private int saved_end;

/**
 * Creates a new DNSInput
 * @param input The byte array to read from
 */
public
DNSInput(byte [] input) {
	byteBuffer = ByteBuffer.wrap(input);
	saved_pos = -1;
	saved_end = -1;
}

/**
 * Creates a new DNSInput from the given {@link ByteBuffer}
 * @param byteBuffer The ByteBuffer
 */
public
DNSInput(ByteBuffer byteBuffer) {
	this.byteBuffer = byteBuffer;
	saved_pos = -1;
	saved_end = -1;
}

/**
 * Returns the current position.
 */
public int
current() {
	return byteBuffer.position();
}

/**
 * Returns the number of bytes that can be read from this stream before
 * reaching the end.
 */
public int
remaining() {
	return byteBuffer.remaining();
}

private void
require(int n) throws WireParseException{
	if (n > remaining()) {
		throw new WireParseException("end of input");
	}
}

/**
 * Marks the following bytes in the stream as active.
 * @param len The number of bytes in the active region.
 * @throws IllegalArgumentException The number of bytes in the active region
 * is longer than the remainder of the input.
 */
public void
setActive(int len) {
	if (len > byteBuffer.capacity() - byteBuffer.position()) {
		throw new IllegalArgumentException("cannot set active " +
						   "region past end of input");
	}
	byteBuffer.limit(byteBuffer.position() + len);
}

/**
 * Clears the active region of the string.  Further operations are not
 * restricted to part of the input.
 */
public void
clearActive() {
	byteBuffer.limit(byteBuffer.capacity());
}

/**
 * Returns the position of the end of the current active region.
 */
public int
saveActive() {
	return byteBuffer.limit();
}

/**
 * Restores the previously set active region.  This differs from setActive() in
 * that restoreActive() takes an absolute position, and setActive takes an
 * offset from the current location.
 * @param pos The end of the active region.
 */
public void
restoreActive(int pos) {
	if (pos > byteBuffer.capacity()) {
		throw new IllegalArgumentException("cannot set active " +
						   "region past end of input");
	}
	byteBuffer.limit(byteBuffer.position());
}

/**
 * Resets the current position of the input stream to the specified index,
 * and clears the active region.
 * @param index The position to continue parsing at.
 * @throws IllegalArgumentException The index is not within the input.
 */
public void
jump(int index) {
	if (index >= byteBuffer.capacity()) {
		throw new IllegalArgumentException("cannot jump past " +
						   "end of input");
	}
	byteBuffer.position(index);
	byteBuffer.limit(byteBuffer.capacity());
}

/**
 * Saves the current state of the input stream.  Both the current position and
 * the end of the active region are saved.
 * @throws IllegalArgumentException The index is not within the input.
 */
public void
save() {
	saved_pos = byteBuffer.position();
	saved_end = byteBuffer.limit();
}

/**
 * Restores the input stream to its state before the call to {@link #save}.
 */
public void
restore() {
	if (saved_pos < 0) {
		throw new IllegalStateException("no previous state");
	}
	byteBuffer.position(saved_pos);
	byteBuffer.limit(saved_end);
	saved_pos = -1;
	saved_end = -1;
}

/**
 * Reads an unsigned 8 bit value from the stream, as an int.
 * @return An unsigned 8 bit value.
 * @throws WireParseException The end of the stream was reached.
 */
public int
readU8() throws WireParseException {
	require(1);
	return (byteBuffer.get() & 0xFF);
}

/**
 * Reads an unsigned 16 bit value from the stream, as an int.
 * @return An unsigned 16 bit value.
 * @throws WireParseException The end of the stream was reached.
 */
public int
readU16() throws WireParseException {
	require(2);
	return (byteBuffer.getShort() & 0xFFFF);
}

/**
 * Reads an unsigned 32 bit value from the stream, as a long.
 * @return An unsigned 32 bit value.
 * @throws WireParseException The end of the stream was reached.
 */
public long
readU32() throws WireParseException {
	require(4);
	return (byteBuffer.getInt() & 0xFFFFFFFFL);
}

/**
 * Reads a byte array of a specified length from the stream into an existing
 * array.
 * @param b The array to read into.
 * @param off The offset of the array to start copying data into.
 * @param len The number of bytes to copy.
 * @throws WireParseException The end of the stream was reached.
 */
public void
readByteArray(byte [] b, int off, int len) throws WireParseException {
	require(len);
	byteBuffer.get(b, off, len);
}

/**
 * Reads a byte array of a specified length from the stream.
 * @return The byte array.
 * @throws WireParseException The end of the stream was reached.
 */
public byte []
readByteArray(int len) throws WireParseException {
	require(len);
	byte [] out = new byte[len];
	byteBuffer.get(out, 0, len);
	return out;
}

/**
 * Reads a byte array consisting of the remainder of the stream (or the
 * active region, if one is set.
 * @return The byte array.
 */
public byte []
readByteArray() {
	int len = remaining();
	byte [] out = new byte[len];
	byteBuffer.get(out, 0, len);
	return out;
}

/**
 * Reads a counted string from the stream.  A counted string is a one byte
 * value indicating string length, followed by bytes of data.
 * @return A byte array containing the string.
 * @throws WireParseException The end of the stream was reached.
 */
public byte []
readCountedString() throws WireParseException {
	int len = readU8();
	return readByteArray(len);
}

}
