package org.fermat.transaction;

import com.google.common.base.Preconditions;
import org.fermat.Main;
import org.fermatj.core.*;

/**
 * Created by rodrigo on 7/25/16.
 */
public class Utils {
    public static final long preminedAmount = 2100000;

    public static void validateGenesisTransaction(Transaction tx) throws GenesisTransactionNotValidException {
        Preconditions.checkNotNull(tx);

        // if we are testing we won't require many blocks to confirm transactions.
        int confirmationBlockDepth;
        if (Main.isIsTestExecution())
            confirmationBlockDepth = 1;
        else
            confirmationBlockDepth = 6;

        // ifg this is a test execution I will leave and ignore the conditions validation
        if (Main.isIsTestExecution())
            return;

        // is already confirmed?
        if (tx.getConfidence().getConfidenceType() != TransactionConfidence.ConfidenceType.BUILDING || tx.getConfidence().getDepthInBlocks() < confirmationBlockDepth)
            throw new GenesisTransactionNotValidException("GenesisTransaction is not yet confirmed (" + tx.getConfidence().getDepthInBlocks() + "/" + confirmationBlockDepth + " blocks)! let's avoid risks and use it when confirmed.");

        // has only one input?
        if (tx.getInputs().size() != 1)
            throw new GenesisTransactionNotValidException("GenesisTransaction must have only one input with the total amount of funds.");
    }

    public static void validateOutgoingTransaction(Transaction tx, ECKey privateKey) throws TransactionErrorException{
        // has only one input?
        if (tx.getInputs().size() != 1)
            throw new TransactionErrorException("Outgoing transaction must only have one input.");

        // too many outputs?
        if (tx.getOutputs().size() > 50)
            throw new TransactionErrorException("Outgoing transaction can't have more than 50 outputs.");


        // too few outputs.
        if (tx.getOutputs().size() < 2)
            throw new TransactionErrorException("Outgoing transaction must have at least 2 outputs! change and destination outputs are required");


        // one output must be to the privateKey address. At least until we exhaust the premined funds
        boolean correctChange = false;
        for (TransactionOutput output : tx.getOutputs()){
            if (output.getScriptPubKey().isSentToAddress())
                if (output.getScriptPubKey().getToAddress(tx.getParams()).equals(privateKey.toAddress(tx.getParams())))
                    correctChange = true;
        }
        if (!correctChange)
            throw new TransactionErrorException("At least one output must return some funds to our own private key.");


    }

    public static ECKey getKey(String dumpedKey){
        Preconditions.checkNotNull(dumpedKey);

        DumpedPrivateKey dumpedPrivateKey = null;
        try {
            dumpedPrivateKey = new DumpedPrivateKey(Main.getNetworkParameters(), dumpedKey);
        } catch (AddressFormatException e) {
            e.printStackTrace();
        }
        return dumpedPrivateKey.getKey();
    }
}
