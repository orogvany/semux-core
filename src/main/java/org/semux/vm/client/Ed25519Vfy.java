package org.semux.vm.client;

import java.util.Arrays;

import org.ethereum.vm.chainspec.PrecompiledContract;
import org.ethereum.vm.chainspec.PrecompiledContractContext;
import org.ethereum.vm.util.Pair;
import org.semux.crypto.CryptoException;
import org.semux.crypto.Hash;
import org.semux.crypto.Key;
import org.semux.crypto.cache.PublicKeyCache;
import org.semux.util.Bytes;

import net.i2p.crypto.eddsa.EdDSAPublicKey;

/**
 * Implementation of https://github.com/ethereum/EIPs/blob/master/EIPS/eip-665.md
 */
public class Ed25519Vfy implements PrecompiledContract
{
    @Override
    public long getGasForData(byte[] bytes)
    {
        return 2000;
    }

    @Override
    public Pair<Boolean, byte[]> execute(PrecompiledContractContext context)
    {

        byte[] data = context.getInternalTransaction().getData();

        if (data == null || data.length != 128)
        {
            return SemuxPrecompiledContracts.failure;
        }

        /**
         * ED25519VFY takes as input 128 octets:
         *
         * message: the 32-octet message that was signed
         * public key: the 32-octet Ed25519 public key of the signer
         * signature: the 64-octet Ed25519 signature
         */
        byte[] message = Arrays.copyOfRange(data, 0, 32);
        byte[] publicKey = Arrays.copyOfRange(data, 32, 64);
        byte[] signature = Arrays.copyOfRange(data, 64, 128);

        byte[] semSignature = Bytes.merge(signature, publicKey);


        boolean isValidSignature = true;
        try
        {
            Key.Signature sig = Key.Signature.fromBytes(semSignature);
            if (sig == null)
            {
                isValidSignature = false;
            } else
            {
                EdDSAPublicKey pubKey = PublicKeyCache.computeIfAbsent(sig.getPublicKey());
                byte[] signatureAddress = Hash.h160(pubKey.getEncoded());

                if (!Key.verify(message, sig))
                {
                    isValidSignature = false;
                }
            }
        } catch (NullPointerException | IllegalArgumentException | CryptoException e)
        {
            isValidSignature = false;
        }

        /**
         * ED25519VFY returns as output 4 octets:
         *
         * 0x00000000 if signature is valid
         * any non-zero value indicates a signature verification failure
         */
        return isValidSignature ? SemuxPrecompiledContracts.success : SemuxPrecompiledContracts.failure;
    }
}
