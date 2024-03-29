package org.bouncycastle.crypto.params;

import java.math.BigInteger;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.bouncycastle.math.Primes;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.Properties;

public class RSAKeyParameters
    extends AsymmetricKeyParameter
{
    // Hexadecimal value of the product of the 131 smallest odd primes from 3 to 743
    private static final BigInteger SMALL_PRIMES_PRODUCT = new BigInteger(
              "8138e8a0fcf3a4e84a771d40fd305d7f4aa59306d7251de54d98af8fe95729a1f"
            + "73d893fa424cd2edc8636a6c3285e022b0e3866a565ae8108eed8591cd4fe8d2"
            + "ce86165a978d719ebf647f362d33fca29cd179fb42401cbaf3df0c614056f9c8"
            + "f3cfd51e474afb6bc6974f78db8aba8e9e517fded658591ab7502bd41849462f",
        16);

    private BigInteger      modulus;
    private BigInteger      exponent;

    public RSAKeyParameters(
        boolean     isPrivate,
        BigInteger  modulus,
        BigInteger  exponent)
    {
        this(isPrivate, modulus, exponent, false);
    }   

    public RSAKeyParameters(
        boolean     isPrivate,
        BigInteger  modulus,
        BigInteger  exponent,
        boolean     isInternal)
    {
        super(isPrivate);

        if (!isPrivate)
        {
            if ((exponent.intValue() & 1) == 0)
            {
                throw new IllegalArgumentException("RSA publicExponent is even");
            }
        }
  
        // only check public keys
        this.modulus = isPrivate ? modulus : validate(modulus, isInternal);
        this.exponent = exponent;
    }

    private static boolean hasAnySmallFactors(BigInteger modulus)
    {
        BigInteger M = modulus, X = SMALL_PRIMES_PRODUCT;
        if (modulus.compareTo(SMALL_PRIMES_PRODUCT) < 0)
        {
            M = SMALL_PRIMES_PRODUCT;
            X = modulus;
        }

        return !BigIntegers.modOddIsCoprimeVar(M, X);
    }

    private static BigInteger validate(BigInteger modulus, boolean isInternal)
    {
        if (isInternal)
        {
            return modulus;
        }

        if ((modulus.intValue() & 1) == 0)
        {
            throw new IllegalArgumentException("RSA modulus is even");
        }

        // If you need to set this you need to have a serious word to whoever is generating
        // your keys.
        if (Properties.isOverrideSet("org.bouncycastle.rsa.allow_unsafe_mod"))
        {
            return modulus;
        }

        int maxBitLength = Properties.asInteger("org.bouncycastle.rsa.max_size", 16384);
        if (maxBitLength < modulus.bitLength())
        {
            throw new IllegalArgumentException("RSA modulus out of range");
        }

        if (hasAnySmallFactors(modulus))
        {
            throw new IllegalArgumentException("RSA modulus has a small prime factor");
        }

        int bits = modulus.bitLength() / 2;
        int iterations = bits >= 1536 ? 3
            : bits >= 1024 ? 4
            : bits >= 512 ? 7
            : 50;

        Primes.MROutput mr = Primes.enhancedMRProbablePrimeTest(modulus, CryptoServicesRegistrar.getSecureRandom(),
            iterations);
        if (!mr.isProvablyComposite())
        {
            throw new IllegalArgumentException("RSA modulus is not composite");
        }

        //validated.add(modulus);
        
        return modulus;
    }

    public BigInteger getModulus()
    {
        return modulus;
    }

    public BigInteger getExponent()
    {
        return exponent;
    }
}
