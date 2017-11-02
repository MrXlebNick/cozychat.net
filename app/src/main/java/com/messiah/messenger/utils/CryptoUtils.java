package com.messiah.messenger.utils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by XlebNick on 27-Oct-17.
 */

public class CryptoUtils {

    public static BigInteger pow(BigInteger base, BigInteger exponent) {
        BigInteger result = BigInteger.ONE;
        while (exponent.signum() > 0) {
            if (exponent.testBit(0)) result = result.multiply(base);
            base = base.multiply(base);
            exponent = exponent.shiftRight(1);
        }
        return result;
    }

    private static BigInteger two = new BigInteger("2");
    public static BigInteger[] generateSeed(){
        BigInteger p = BigInteger.probablePrime(997, new Random());

        BigInteger m = p.subtract(BigInteger.ONE);
        BigInteger g = BigInteger.ZERO;
        Map<BigInteger, BigInteger> primeFactor = getPrimeFactor(m);
        for (Map.Entry<BigInteger, BigInteger> map : primeFactor.entrySet()) {
            primeFactor.put(map.getKey(), m.divide( map.getKey()));
        }
        for (BigInteger i = new BigInteger("3"); m.compareTo(i) >=0 ; i = i.add(two)) {
            boolean notPrimeRoot = false;
            for (Map.Entry<BigInteger, BigInteger> map : primeFactor.entrySet()) {
                if(i.modPow(map.getValue(), p).equals(BigInteger.ONE))
                    notPrimeRoot = true;
            }
            if (!notPrimeRoot) {
                g = i;
                break;
            }
        }

        return new BigInteger[]{p, g};

    }

    private static Map<BigInteger, BigInteger> getPrimeFactor(BigInteger p) {
        Map<BigInteger, BigInteger> map = new HashMap<>();
        while (p.mod(two).equals(BigInteger.ZERO)) {

            insertToMap(two, map);
            p = p.divide(two);
        }

        for (BigInteger i = new BigInteger("3"); p.mod(two).compareTo(i) > 0; i = i.add(two)) {
            while (p.mod(i).equals(BigInteger.ZERO)) {
                insertToMap(i, map);
                p = p.divide(i);
            }
        }

        if (p.compareTo(two) > 0)
            insertToMap(p, map);
        return map;
    }

    private static void insertToMap(BigInteger i, Map<BigInteger, BigInteger> map) {
        if (map.get(i) != null) {
            map.put(i, map.get(i).add(BigInteger.ONE));
        } else {
            map.put(i, BigInteger.ONE);
        }
    }

}
