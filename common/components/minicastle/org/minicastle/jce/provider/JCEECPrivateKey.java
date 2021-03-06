package org.minicastle.jce.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.minicastle.asn1.ASN1Sequence;
import org.minicastle.asn1.DEREncodable;
import org.minicastle.asn1.DERInteger;
import org.minicastle.asn1.DERObject;
import org.minicastle.asn1.DERObjectIdentifier;
import org.minicastle.asn1.DEROutputStream;
import org.minicastle.asn1.pkcs.PrivateKeyInfo;
import org.minicastle.asn1.sec.ECPrivateKeyStructure;
import org.minicastle.asn1.x509.AlgorithmIdentifier;
import org.minicastle.asn1.x9.X962NamedCurves;
import org.minicastle.asn1.x9.X962Parameters;
import org.minicastle.asn1.x9.X9ECParameters;
import org.minicastle.asn1.x9.X9ObjectIdentifiers;
import org.minicastle.crypto.params.ECDomainParameters;
import org.minicastle.crypto.params.ECPrivateKeyParameters;
import org.minicastle.jce.interfaces.ECPrivateKey;
import org.minicastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.minicastle.jce.spec.ECNamedCurveParameterSpec;
import org.minicastle.jce.spec.ECParameterSpec;
import org.minicastle.jce.spec.ECPrivateKeySpec;

public class JCEECPrivateKey
    implements ECPrivateKey, PKCS12BagAttributeCarrier
{
    private String          algorithm = "EC";
    private BigInteger      d;
    private ECParameterSpec ecSpec;

    private Hashtable   pkcs12Attributes = new Hashtable();
    private Vector      pkcs12Ordering = new Vector();

    protected JCEECPrivateKey()
    {
    }

    JCEECPrivateKey(
        ECPrivateKey    key)
    {
        this.d = key.getD();
        this.algorithm = key.getAlgorithm();
        this.ecSpec = key.getParams();
    }

    JCEECPrivateKey(
        String              algorithm,
        ECPrivateKeySpec    spec)
    {
        this.algorithm = algorithm;
        this.d = spec.getD();
        this.ecSpec = spec.getParams();
    }

    JCEECPrivateKey(
        String                  algorithm,
        ECPrivateKeyParameters  params,
        ECParameterSpec         spec)
    {
        ECDomainParameters      dp = params.getParameters();

        this.algorithm = algorithm;
        this.d = params.getD();

        if (spec == null)
        {
            this.ecSpec = new ECParameterSpec(
                            dp.getCurve(),
                            dp.getG(),
                            dp.getN(),
                            dp.getH(),
                            dp.getSeed());
        }
        else
        {
            this.ecSpec = spec;
        }
    }

    JCEECPrivateKey(
        PrivateKeyInfo      info)
    {
        X962Parameters      params = new X962Parameters((DERObject)info.getAlgorithmId().getParameters());

        if (params.isNamedCurve())
        {
            DERObjectIdentifier oid = (DERObjectIdentifier)params.getParameters();
            X9ECParameters      ecP = X962NamedCurves.getByOID(oid);

            ecSpec = new ECNamedCurveParameterSpec(
                                        X962NamedCurves.getName(oid),
                                        ecP.getCurve(),
                                        ecP.getG(),
                                        ecP.getN(),
                                        ecP.getH(),
                                        ecP.getSeed());
        }
        else
        {
            X9ECParameters          ecP = new X9ECParameters((ASN1Sequence)params.getParameters());
            ecSpec = new ECParameterSpec(ecP.getCurve(),
                                            ecP.getG(),
                                            ecP.getN(),
                                            ecP.getH(),
                                            ecP.getSeed());
        }

        if (info.getPrivateKey() instanceof DERInteger)
        {
            DERInteger          derD = (DERInteger)info.getPrivateKey();

            this.d = derD.getValue();
        }
        else
        {
            ECPrivateKeyStructure   ec = new ECPrivateKeyStructure((ASN1Sequence)info.getPrivateKey());

            this.d = ec.getKey();
        }
    }

    public String getAlgorithm()
    {
        return algorithm;
    }

    /**
     * return the encoding format we produce in getEncoded().
     *
     * @return the string "PKCS#8"
     */
    public String getFormat()
    {
        return "PKCS#8";
    }

    /**
     * Return a PKCS8 representation of the key. The sequence returned
     * represents a full PrivateKeyInfo object.
     *
     * @return a PKCS8 representation of the key.
     */
    public byte[] getEncoded()
    {
        ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
        DEROutputStream         dOut = new DEROutputStream(bOut);
        X962Parameters          params = null;

        if (ecSpec instanceof ECNamedCurveParameterSpec)
        {
            params = new X962Parameters(X962NamedCurves.getOID(((ECNamedCurveParameterSpec)ecSpec).getName()));
        }
        else
        {
            X9ECParameters          ecP = new X9ECParameters(
                                            ecSpec.getCurve(),
                                            ecSpec.getG(),
                                            ecSpec.getN(),
                                            ecSpec.getH(),
                                            ecSpec.getSeed());
            params = new X962Parameters(ecP);
        }

        PrivateKeyInfo          info = new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, params.getDERObject()), new ECPrivateKeyStructure(this.getD()).getDERObject());

        try
        {
            dOut.writeObject(info);
            dOut.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error encoding EC private key");
        }

        return bOut.toByteArray();
    }

    public ECParameterSpec getParams()
    {
        return ecSpec;
    }

    public BigInteger getD()
    {
        return d;
    }

/*
    private void readObject(
        ObjectInputStream   in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        boolean named = in.readBoolean();

        if (named)
        {
            ecSpec = new ECNamedCurveParameterSpec(
                        in.readUTF(),
                        (ECCurve)in.readObject(),
                        (ECPoint)in.readObject(),
                        (BigInteger)in.readObject(),
                        (BigInteger)in.readObject(),
                        (byte[])in.readObject());
        }
        else
        {
            ecSpec = new ECParameterSpec(
                        (ECCurve)in.readObject(),
                        (ECPoint)in.readObject(),
                        (BigInteger)in.readObject(),
                        (BigInteger)in.readObject(),
                        (byte[])in.readObject());
        }
    }

    private void writeObject(
        ObjectOutputStream  out)
        throws IOException
    {
        out.defaultWriteObject();

        if (this.ecSpec instanceof ECNamedCurveParameterSpec)
        {
            ECNamedCurveParameterSpec   namedSpec = (ECNamedCurveParameterSpec)ecSpec;

            out.writeBoolean(true);
            out.writeUTF(namedSpec.getName());
        }
        else
        {
            out.writeBoolean(false);
        }

        out.writeObject(ecSpec.getCurve());
        out.writeObject(ecSpec.getG());
        out.writeObject(ecSpec.getN());
        out.writeObject(ecSpec.getH());
        out.writeObject(ecSpec.getSeed());
    }
*/

    public void setBagAttribute(
        DERObjectIdentifier oid,
        DEREncodable        attribute)
    {
        pkcs12Attributes.put(oid, attribute);
        pkcs12Ordering.addElement(oid);
    }

    public DEREncodable getBagAttribute(
        DERObjectIdentifier oid)
    {
        return (DEREncodable)pkcs12Attributes.get(oid);
    }

    public Enumeration getBagAttributeKeys()
    {
        return pkcs12Ordering.elements();
    }
}
