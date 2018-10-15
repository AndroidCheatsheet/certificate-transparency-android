/*
 * Copyright 2018 Babylon Healthcare Services Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.certificatetransparency.ctlog

import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.crypto.tls.TlsUtils
import org.certificatetransparency.ctlog.serialization.CTConstants
import org.certificatetransparency.ctlog.serialization.Deserializer
import org.certificatetransparency.ctlog.serialization.model.SignedCertificateTimestamp
import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.ArrayList

@Throws(IOException::class)
fun X509Certificate.signedCertificateTimestamps(): List<SignedCertificateTimestamp> {
    val bytes = getExtensionValue(CTConstants.SCT_CERTIFICATE_OID)
    val p = ASN1Primitive.fromByteArray(ASN1OctetString.getInstance(bytes).octets) as DEROctetString

    // These are serialized SCTs, we must de-serialize them into an array with one SCT each
    return parseSCTsFromCertExtension(p.octets).toMutableList()
}

@Throws(IOException::class)
private fun parseSCTsFromCertExtension(extensionvalue: ByteArray): Array<SignedCertificateTimestamp> {
    val sctList = ArrayList<SignedCertificateTimestamp>()
    val bis = ByteArrayInputStream(extensionvalue)
    TlsUtils.readUint16(bis) // first one is the length of all SCTs concatenated, we don't actually need this
    while (bis.available() > 2) {
        val sctBytes = TlsUtils.readOpaque16(bis)
        // System.out.println("Read SCT bytes (excluding length): " + sctBytes.length);
        sctList.add(Deserializer.parseSCTFromBinary(ByteArrayInputStream(sctBytes)))
    }
    return sctList.toTypedArray()
}