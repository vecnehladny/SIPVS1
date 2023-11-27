package sk.stu.fiit.sipvs1.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ValidatorConstants {

    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
    public static final String UTF8_BOM = "\uFEFF";

    //4.5
    public static final List<String> SIGNATURE_METHODS = Arrays.asList(
            "http://www.w3.org/2000/09/xmldsig#dsa-sha1",
            "http://www.w3.org/2000/09/xmldsig#rsa-sha1",
            "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
            "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384",
            "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512");

    //4.3.1.1
    public static final List<String> CANONICALIZATION_METHODS = List.of(
            "http://www.w3.org/TR/2001/REC-xml-c14n-20010315");

    //4.3.1.3.1.
    public static final List<String> TRANSFORM_METHODS = List.of(
            "http://www.w3.org/TR/2001/REC-xml-c14n-20010315");

    //4.5
    public static final List<String> DIGEST_METHODS = Arrays.asList(
            "http://www.w3.org/2000/09/xmldsig#sha1",
            "http://www.w3.org/2001/04/xmldsig-more#sha224",
            "http://www.w3.org/2001/04/xmlenc#sha256",
            "http://www.w3.org/2001/04/xmldsig-more#sha384",
            "http://www.w3.org/2001/04/xmlenc#sha512");

    //4.3.4.1.
    public static final List<String> MANIFEST_TRANSFORM_METHODS = Arrays.asList(
            "http://www.w3.org/TR/2001/REC-xml-c14n-20010315",
            "http://www.w3.org/2000/09/xmldsig#base64");


    public static final Map<String, String> REFERENCES;

    // BOD 4.3.1
    static {
        REFERENCES = Map.of("ds:KeyInfo",
                            "http://www.w3.org/2000/09/xmldsig#Object",
                            "ds:SignatureProperties",
                            "http://www.w3.org/2000/09/xmldsig#SignatureProperties",
                            "xades:SignedProperties",
                            "http://uri.etsi.org/01903#SignedProperties",
                            "ds:Manifest",
                            "http://www.w3.org/2000/09/xmldsig#Manifest");
    }

    public static final Map<String, String> DIGEST_ALG;

    static {
        DIGEST_ALG = Map.of("http://www.w3.org/2000/09/xmldsig#sha1",
                            "SHA-1",
                            "http://www.w3.org/2001/04/xmldsig-more#sha224",
                            "SHA-224",
                            "http://www.w3.org/2001/04/xmlenc#sha256",
                            "SHA-256",
                            "http://www.w3.org/2001/04/xmldsig-more#sha384",
                            "SHA-384",
                            "http://www.w3.org/2001/04/xmlenc#sha512",
                            "SHA-512");
    }

    public static final Map<String, String> SIGN_ALG;

    static {
        SIGN_ALG = Map.of("http://www.w3.org/2000/09/xmldsig#dsa-sha1",
                          "SHA1withDSA",
                          "http://www.w3.org/2000/09/xmldsig#rsa-sha1",
                          "SHA1withRSA/ISO9796-2",
                          "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
                          "SHA256withRSA",
                          "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384",
                          "SHA384withRSA",
                          "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512",
                          "SHA512withRSA");
    }
}
