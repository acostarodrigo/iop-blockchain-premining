package org.fermat.fermatTransaction;

import com.google.common.base.Preconditions;
import org.fermat.Main;
import org.fermatj.core.Coin;
import org.fermatj.core.ECKey;
import org.fermatj.core.TransactionOutput;
import org.fermatj.script.Script;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by rodrigo on 7/25/16.
 */
public class FermatTransaction {
    private String alias;
    private ECKey publicKey;
    private Coin fermats;
    private boolean isTimeContrained;
    private int daysForPayment;
    private Date paymentDate;
    private long paymentEpochTime;
    private TransactionOutput output;
    private Script redeemScript;

    /**
     * default constructor
     * @param alias
     * @param publicKey
     * @param fermats
     */
    public FermatTransaction(String alias, ECKey publicKey, Coin fermats) {
        Preconditions.checkNotNull(alias);
        Preconditions.checkNotNull(publicKey);
        Preconditions.checkNotNull(fermats);

        this.alias = alias;
        this.publicKey = publicKey;
        this.fermats = fermats;

        this.isTimeContrained = false;
    }

    /**
     * constructor with date
     * @param alias
     * @param publicKey
     * @param fermats
     * @param daysForPayment
     */
    public FermatTransaction(String alias, ECKey publicKey, Coin fermats, int daysForPayment) {
        Preconditions.checkNotNull(alias);
        Preconditions.checkNotNull(publicKey);
        Preconditions.checkNotNull(fermats);
        Preconditions.checkNotNull(daysForPayment);

        this.alias = alias;
        this.publicKey = publicKey;
        this.fermats = fermats;
        this.daysForPayment = daysForPayment;

        // sets the time properties.
        this.isTimeContrained = true;
        this.paymentEpochTime = getTransactionEpochTime(daysForPayment);
        this.paymentDate = new Date(this.paymentEpochTime);
    }

    /**
     * Adds the passed passed days to the Main Epoch Time of when the process started.
     * @return
     */
    private long getTransactionEpochTime(int daysForPayment) {
        // gets the date of the start of the process from the Main class
        Date startDate = new Date(Main.getEpocTime());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        // add the amount of dates
        calendar.add(Calendar.DATE, daysForPayment);

        //convert back to epoch
        return calendar.getTime().getTime();
    }

    public TransactionOutput getOutput() {
        return output;
    }

    public void setOutput(TransactionOutput output) {
        this.output = output;
    }

    public String getAlias() {
        return alias;
    }

    public ECKey getPublicKey() {
        return this.publicKey;
    }

    public Coin getFermats() {
        return fermats;
    }

    public boolean isTimeContrained() {
        return isTimeContrained;
    }

    public int getDaysForPayment() {
        return daysForPayment;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public long getPaymentEpochTime() {
        return paymentEpochTime;
    }

    public Script getRedeemScript() {
        return redeemScript;
    }

    public void setRedeemScript(Script redeemScript) {
        this.redeemScript = redeemScript;
    }

    @Override
    public String toString() {
        return "FermatTransaction{" +
                "alias='" + alias + '\'' +
                ", fermats=" + fermats +
                ", paymentDate=" + paymentDate +
                ", paymentEpochTime=" + paymentEpochTime +
                '}';
    }
}
