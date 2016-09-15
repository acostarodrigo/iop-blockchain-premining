package org.fermat.fermatTransaction;

import com.google.common.base.Preconditions;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.fermat.Main;
import org.fermat.fermatTransaction.inputFileManagement.InputFileReader;
import org.fermat.transaction.Utils;
import org.fermatj.core.Address;
import org.fermatj.core.AddressFormatException;
import org.fermatj.core.Coin;
import org.fermatj.core.ECKey;
import org.spongycastle.util.encoders.Hex;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rodrigo on 7/25/16.
 */
public class FermatTransactionBuilder {
    //class variables
    private List<FermatTransaction> fermatTransactions;
    private final InputFileReader fileReader;
    private long totalFermats;

    //class constants
    private final File inputFile;
    private int EXPECTED_COLUMN_COUNT = 4;


    /**
     * column headers enum
     */
    private enum ColumnHeaders{
        Name (0), PublicKey (1), Fermats (2), DaysForPayment (3);

        private int index;

        ColumnHeaders(int index) {this.index = index;}

        public int getIndex (){return this.index;}

        public static ColumnHeaders getByIndex(int index){
            switch (index){
                case 0:
                    return Name;
                case 1:
                    return PublicKey;
                case 2:
                    return Fermats;
                case 3:
                    return DaysForPayment;
                default: return null;
            }
        }

    }

    /**
     * constructor
     * @param inputFile
     */
    public FermatTransactionBuilder(File inputFile) throws  TransactionsInputFileNotValidException {
        Preconditions.checkNotNull(inputFile);

        this.inputFile = inputFile;
        this.fileReader = new InputFileReader(this.inputFile);

        //isntantiate an empty list
        fermatTransactions = new ArrayList<>();

        this.validateInputFileStructure();
    }


    public List<FermatTransaction> getFermatTransactions() {
        return fermatTransactions;
    }

    /**
     * makes sure of the format of the input file is correct
     */
    private void validateInputFileStructure() throws TransactionsInputFileNotValidException {
        /**
         * Expected format for file is name, address, fermats, days for payment
         * example: Rodrigo Acosta, pK2j9ojKzoAhqUz6oXAWKDkE6GxcVpx7jU, 10000, 200
         * We need to validate all records are ok before any attemp to create transactions.
         */

        try {
            fileReader.read();
        } catch (FileNotFoundException e) {
            throw new TransactionsInputFileNotValidException("The specified file " + inputFile.getAbsolutePath() + " does not exists.");
        }

        // validate column count
        List<String> columns = fileReader.getHeader();
        if (columns.size() != EXPECTED_COLUMN_COUNT)
            throw new TransactionsInputFileNotValidException("The amount of columns of the file is not valid. " + columns.toString());



        // validate columns name based in position
        for (int i=0; i<4; i++){
            if (!columns.get(i).equalsIgnoreCase(ColumnHeaders.getByIndex(i).name()))
                throw new TransactionsInputFileNotValidException("Missing column " +  ColumnHeaders.getByIndex(i).toString() + " on input file. Found columns are " + columns.toString());
        }

        //validate we have at least one transaction
        if (fileReader.getRows().size() <  2)
            throw new TransactionsInputFileNotValidException("No transactions detected on the input file");


    }


    /**
     * generates the list of Fermat Transactions by reading from the file
     * @throws IOException
     * @throws AddressFormatException
     */
    public void generateTransactions() throws AddressFormatException, TransactionsInputFileNotValidException {
        int recordCount = fileReader.getRows().size();

        for (List<String> row : fileReader.getRows()){
            String name = row.get(0);

            // create the publicKey address
            ECKey publicKey = ECKey.fromPublicOnly(Hex.decode(row.get(1)));

            // get the amount of Fermats
            long fermatQty = Long.parseLong(row.get(2));
            Coin fermats = getFermatCoin(fermatQty);

            totalFermats = totalFermats + fermats.getValue();

            FermatTransaction fermatTransaction;

            // create the fermat transaction, adding the date column if needed.
            if (row.size() == 4) {
                int days = Integer.parseInt(row.get(3));
                if (days > 0)
                    fermatTransaction = new FermatTransaction(name, publicKey, fermats, days);
                else
                    fermatTransaction = new FermatTransaction(name, publicKey, fermats);
            } else
                fermatTransaction = new FermatTransaction(name, publicKey, fermats);

            fermatTransactions.add(fermatTransaction);
        }
    }

    /**
     * validates the amount of fermats for this transaction is within the expected values.
     * @param fermatQty
     * @return
     * @throws TransactionsInputFileNotValidException
     */
    private Coin getFermatCoin(long fermatQty) throws TransactionsInputFileNotValidException {
        if (fermatQty == 0)
            throw new TransactionsInputFileNotValidException("The amount of fermats can't be zero");

        if (fermatQty > Utils.preminedAmount)
            throw new TransactionsInputFileNotValidException("The amount of fermats can't be higher than the total amount of premined value");

        return Coin.COIN.multiply(fermatQty);
    }

    public long getTotalFermats() {
        return totalFermats;
    }
}
