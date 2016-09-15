package org.fermat.fermatTransaction;

import com.google.common.base.Preconditions;
import org.fermat.Main;
import org.fermatj.core.Coin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rodrigo on 7/26/16.
 */
public class FermatTransactionsSummary {
    private long totalFermats;
    private int totalTransactions;
    private Map<String, Long> userDetail;
    private final List<FermatTransaction> fermatTransactionList;

    /**
     * constructor
     * @param fermatTransactionList
     */
    public FermatTransactionsSummary(List<FermatTransaction> fermatTransactionList) {
        Preconditions.checkNotNull(fermatTransactionList);

        this.fermatTransactionList = fermatTransactionList;

        //initialize map
        userDetail = new HashMap<>();

    }

    public void calculateSummary(){
        //erase previous summary
        resetSummary();

        // calculate amount of transactions
        this.totalTransactions = fermatTransactionList.size();

        for (FermatTransaction fermatTransaction : this.fermatTransactionList){
            //calculate total fermats to distribute
            this.totalFermats = totalFermats + fermatTransaction.getFermats().getValue();

            // calculate user summary
            String alias = fermatTransaction.getAlias();
            long fermats = fermatTransaction.getFermats().getValue();

            if (userDetail.containsKey(alias)){
                long summaryFermats = userDetail.get(alias);
                userDetail.remove(alias);
                userDetail.put(alias, fermats + summaryFermats);
            }
            else
                userDetail.put(alias, fermats);
        }
    }

    private void resetSummary() {
        this.totalFermats = 0;
        this.totalTransactions = 0;
        this.userDetail.clear();
    }

    public long getTotalFermats() {
        return totalFermats;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public Map<String, Long> getUserDetail() {
        return userDetail;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append("Start time of process (Epoch Time): " + Main.getEpocTime());
        output.append(System.lineSeparator());
        output.append("Total IoPs to distribute on a single transaction = " + Coin.valueOf(totalFermats).toFriendlyString());
        output.append(System.lineSeparator());
        output.append("Total distributions included on a single transaction = " + totalTransactions);
        output.append(System.lineSeparator());
        output.append("User Distribution: ");
        output.append(System.lineSeparator());
        for (Map.Entry<String,Long> entry : userDetail.entrySet()){
            output.append(entry.getKey() + " - " + Coin.valueOf(entry.getValue()).toFriendlyString());
            output.append(System.lineSeparator());
        }
        return output.toString();
    }
}
