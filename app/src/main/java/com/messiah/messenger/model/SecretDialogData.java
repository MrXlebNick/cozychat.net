package com.messiah.messenger.model;

import android.util.Log;

import com.messiah.messenger.utils.Utils;
import com.orm.SugarRecord;

import org.bouncycastle.jcajce.provider.asymmetric.dh.BCDHPrivateKey;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Created by Dinara on 30-Oct-17.
 */

public class SecretDialogData extends SugarRecord {
    public String opponentNumber;
    public String dialogId;
    public String privateKey;
    public boolean isComplete;
    public byte[] secret;


    @Override
    public long save() {
        if (SecretDialogData.find(SecretDialogData.class, "dialog_id = \""+ dialogId+"\"")
                .isEmpty()) {
            return super.save();
        } else {
            Log.d("***", "try to save duplicates");
        }
        return -1;
    }

    public PrivateKey getPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {

        KeyFactory clientKeyFac = KeyFactory.getInstance("DH");
        PKCS8EncodedKeySpec x509KeySpec = new PKCS8EncodedKeySpec(new BigInteger(privateKey,16).toByteArray());
        return clientKeyFac.generatePrivate(x509KeySpec);
    }

    public void setPrivateKey(PrivateKey privateKey) {
        Log.d("shit", privateKey.getAlgorithm() + " " + privateKey.getFormat());

        this.privateKey = Utils.bytesToHex(privateKey.getEncoded());
    }
}
