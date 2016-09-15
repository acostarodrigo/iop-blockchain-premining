package org.fermat.transaction;

import com.google.common.base.Preconditions;
import org.fermat.Main;
import org.fermat.fermatTransaction.FermatTransaction;
import org.fermatj.core.*;
import org.fermatj.crypto.TransactionSignature;
import org.fermatj.script.Script;
import org.fermatj.script.ScriptBuilder;
import org.fermatj.script.ScriptChunk;
import org.fermatj.script.ScriptOpCodes;
import org.fermatj.wallet.RedeemData;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by rodrigo on 7/25/16.
 */
public class TransactionBuilder {
    // class contants
    private final List<FermatTransaction> fermatTransactionList;
    private final Sha256Hash genesisTxhash;
    private final Wallet wallet;
    private final Transaction transaction;
    private final Transaction genesisTransaction;

    /**
     * Constructor
     * The ID of the Genesis Transaction that has all the preMining funds.
     * @param genesisTxhash
     */
    public TransactionBuilder(Sha256Hash genesisTxhash, Wallet wallet, List<FermatTransaction> fermatTransactions) {
        this.genesisTxhash = genesisTxhash;
        this.wallet = wallet;
        this.fermatTransactionList = fermatTransactions;

        //validate the transaction exists in the wallet.
        Preconditions.checkNotNull(this.wallet.getTransaction(genesisTxhash));

        // gets the genesis transaction
        genesisTransaction = wallet.getTransaction(genesisTxhash);
        try {
            Utils.validateGenesisTransaction(genesisTransaction);
        } catch (GenesisTransactionNotValidException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        //initialize transaction
        transaction = new Transaction(wallet.getParams());

        //add the input
        addGenesisTransactionInput();
    }

    /**
     * constructor to generate the redeem scripts only
     */
    public TransactionBuilder() {
        fermatTransactionList = null;
        genesisTxhash = null;
        wallet = null;
        transaction = null;
        genesisTransaction = null;
    }

    /**
     * creates the input pointing to the output of our own key
     */
    private void addGenesisTransactionInput(){
        //identify my output on the genesisTransaction
        TransactionOutput output = getMyOutput();
        Preconditions.checkNotNull(output);

        TransactionOutPoint outPoint = new TransactionOutPoint(wallet.getParams(), output);
        Preconditions.checkNotNull(outPoint);

        TransactionInput input = new TransactionInput(this.wallet.getParams(), transaction, output.getScriptBytes(), outPoint);
        transaction.addInput(input);
    }

    /**
     * will get the output that it was sent to me if any.
     * If no outputs where sent to me then there is an error.
     * @return
     */
    private TransactionOutput getMyOutput() {
        for (TransactionOutput output : genesisTransaction.getOutputs()){
            if (output.isMine(wallet))
                return output;
        }
        return null;
    }

    /**
     * getter of the transaction we are building,
     * @return
     */
    public Transaction getTransaction() {
        return transaction;
    }

    /**
     * adds a fermat transaction which will generate the outputs into the transaction
     * @param fermatTransaction
     */
    public void addFermatTransaction (FermatTransaction fermatTransaction){
        Preconditions.checkNotNull(fermatTransaction);

        if (fermatTransaction.isTimeContrained())
            addTimeConstrainedTransaction(fermatTransaction);
        else
            addNotTimeContrainedTransaction(fermatTransaction);

    }


    private void addNotTimeContrainedTransaction(FermatTransaction fermatTransaction) {
        Coin fermats = fermatTransaction.getFermats();
        // instead of using the public key, I will send it to the address
        Address address = fermatTransaction.getPublicKey().toAddress(Main.getNetworkParameters());
        TransactionOutput output = new TransactionOutput(this.wallet.getParams(), this.genesisTransaction, fermats, address);
        transaction.addOutput(output);

        fermatTransaction.setOutput(output);
        fermatTransaction.setRedeemScript(output.getScriptPubKey());
    }

    /**
     * Generates a time constrained redeem script and adds it to the IoP transaction and fermat transaction.
     * @param fermatTransaction
     */
    public void addTimeConstrainedTransaction(FermatTransaction fermatTransaction) {
        Coin fermats = fermatTransaction.getFermats();


        byte[] epochTime = longToBytes(fermatTransaction.getPaymentEpochTime());
        ScriptChunk pushData4 = new ScriptChunk(8, epochTime);
        ScriptChunk locktimeVerify = new ScriptChunk(ScriptOpCodes.OP_NOP2, null);
        ScriptChunk drop = new ScriptChunk(ScriptOpCodes.OP_DROP, null);
        ScriptChunk pushData = new ScriptChunk(33, fermatTransaction.getPublicKey().getPubKey());
        ScriptChunk checksig = new ScriptChunk(ScriptOpCodes.OP_CHECKSIG, null);


        ScriptBuilder scriptBuilder = new ScriptBuilder();
        scriptBuilder.addChunk(pushData4);
        scriptBuilder.addChunk(locktimeVerify);
        scriptBuilder.addChunk(drop);
        scriptBuilder.addChunk(pushData);
        scriptBuilder.addChunk(checksig);

        Script redeemScript = scriptBuilder.build();

        if (transaction != null && genesisTransaction != null){
            Script outputScript = ScriptBuilder.createP2SHOutputScript(redeemScript);
            TransactionOutput output = new TransactionOutput(this.wallet.getParams(), this.genesisTransaction, fermats, outputScript.getProgram());

            transaction.addOutput(output);
            fermatTransaction.setOutput(output);
        }

        //add the output that will be used for this transaction
        fermatTransaction.setRedeemScript(redeemScript);
    }


    private byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    /**
     * completes the transaction and prepares it for sending
     * @throws InsufficientMoneyException
     */
    public void completeTransaction() throws InsufficientMoneyException, TransactionErrorException {
        //let the sendRequest complete the change outputs and signature
        Wallet.SendRequest sendRequest = Wallet.SendRequest.forTx(transaction);
        ECKey privateKey = wallet.getImportedKeys().get(0);
        sendRequest.changeAddress = privateKey.toAddress(wallet.getParams());

        sendRequest.ensureMinRequiredFee = false;
        sendRequest.fee = Coin.ZERO;

        wallet.completeTx(sendRequest);

        //once completed let's make sure everything is ok
        Utils.validateOutgoingTransaction(sendRequest.tx, privateKey);
    }

}
