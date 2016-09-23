package org.fermat.fermatTransaction;

import org.fermat.Main;
import org.fermatj.core.Transaction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by rodrigo on 9/13/16.
 */
public class ExecutionLogger {
    private final FermatTransactionBuilder fermatTransactionBuilder;
    private final Transaction transaction;
    private final File outputFile;
    private List<String> output;

    //constructor
    public ExecutionLogger(FermatTransactionBuilder fermatTransactionBuilder, Transaction transaction) {
        this.fermatTransactionBuilder = fermatTransactionBuilder;
        this.transaction = transaction;

        //the file that we will use to store the output-
        this.outputFile = new File("preMiningDistributor.output");
    }

    public void saveOutput() throws IOException {
        this.output = generateOutput();
        Path path = Paths.get(outputFile.getPath());
        if (this.getOutputFile().exists())
            Files.write(path, output, Charset.forName("UTF-8"), StandardOpenOption.APPEND);
        else
            Files.write(path, output, Charset.forName("UTF-8"), StandardOpenOption.CREATE);
    }

    private List<String> generateOutput() {
        List<String> lines = new ArrayList<>();
        lines.add(getCurrentDate());

        lines.add("Execution epoch time: " + Main.getEpocTime());
        lines.add(System.lineSeparator());
        lines.add("File content used:");
        lines.add(System.lineSeparator());
        for (String transaction : getFileContent()){
            lines.add(transaction);
        }
        lines.add(System.lineSeparator());
        lines.add("Transaction broadcasted: " + transaction.getHashAsString());
        lines.add(System.lineSeparator());
        return lines;
    }

    private List<String> getFileContent() {
        List<String> lines = new ArrayList<>();
        lines.add("Name,PublicKey,Fermats,DaysForPayment"); //columns header
        for (FermatTransaction fermatTransaction : fermatTransactionBuilder.getFermatTransactions()){
            lines.add(fermatTransaction.getAlias()+","+fermatTransaction.getAddress().toString()+","+fermatTransaction.getFermats().toPlainString()+","+fermatTransaction.getDaysForPayment());
        }
        return lines;
    }

    private String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        return dateFormat.format(date);
    }

    public List<String> getOutput() {
        return output;
    }

    public File getOutputFile() {
        return outputFile;
    }
}
