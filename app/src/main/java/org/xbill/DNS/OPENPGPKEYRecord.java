package org.xbill.DNS;

import java.io.IOException;

import org.xbill.DNS.utils.base64;

/**
 * OPENPGPKEY Record - Stores an OpenPGP certificate associated with a name.
 * RFC 7929.
 * 
 * @author Brian Wellington
 * @author Valentin Hauner
 *
 */
public class OPENPGPKEYRecord extends Record {

private static final long serialVersionUID = -1277262990243423062L;

private byte [] cert;

OPENPGPKEYRecord() {}

Record
getObject() {
	return new OPENPGPKEYRecord();
}

/**
 * Creates an OPENPGPKEY Record from the given data
 * 
 * @param cert Binary data representing the certificate
 */
public
OPENPGPKEYRecord(Name name, int dclass, long ttl, byte [] cert)
{
	super(name, Type.OPENPGPKEY, dclass, ttl);
	this.cert = cert;
}

void
rrFromWire(DNSInput in) throws IOException {
	cert = in.readByteArray();
}

void
rdataFromString(Tokenizer st, Name origin) throws IOException {
	cert = st.getBase64();
}

/**
 * Converts rdata to a String
 */
String
rrToString() {
	StringBuffer sb = new StringBuffer();
	if (cert != null) {
		if (Options.check("multiline")) {
			sb.append("(\n");
			sb.append(base64.formatString(cert, 64, "\t", true));
		} else {
			sb.append(base64.toString(cert));
		}
	}
	return sb.toString();
}

/**
 * Returns the binary representation of the certificate
 */
public byte []
getCert()
{
	return cert;
}

void
rrToWire(DNSOutput out, Compression c, boolean canonical) {
	out.writeByteArray(cert);
}

}
