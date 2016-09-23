package org.fermat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.cli.*;
import org.fermat.blockchain.CantConnectToFermatBlockchainException;
import org.fermat.blockchain.FermatNetwork;
import org.fermat.fermatTransaction.ExecutionLogger;
import org.fermat.fermatTransaction.FermatTransaction;
import org.fermat.fermatTransaction.FermatTransactionBuilder;
import org.fermat.fermatTransaction.FermatTransactionsSummary;
import org.fermat.transaction.TransactionBuilder;
import org.fermat.transaction.TransactionErrorException;
import org.fermatj.core.*;
import org.fermatj.params.MainNetParams;
import org.fermatj.params.RegTestParams;
import org.fermatj.params.TestNet3Params;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.Console;
import java.io.File;
import java.io.IOException;

public class Main {
    // static variables
    private static NetworkParameters networkParameters; //the network parameters of the network
    private static long EPOC_TIME; // the epoch time to use for the process
    private static boolean isTestExecution = false; //if this is a test execution to avoid controls
    private static HelpFormatter formatter;
    private static CommandLine cmd;
    private static Options options;
    private static FermatTransactionBuilder fermatTransactionBuilder;

    //gets the logger
    public static Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    public static NetworkParameters getNetworkParameters(){
        return networkParameters;
    }

    public static void main(String[] args) throws CantConnectToFermatBlockchainException, TransactionErrorException {
        //input variables
//        String pubkey;
//        try {
//            pubkey = new DumpedPrivateKey(RegTestParams.get(), "CHh4wo86cPPS9ecUAUqY2FnRMnxb1c6fyqtFjRuKj3gP84y99pKT").getKey().getPublicKeyAsHex();
//        } catch (AddressFormatException e) {
//            e.printStackTrace();
//        }
        String privateKey;
        File  inputFile;

        options = defineOptions();

        CommandLineParser parser = new DefaultParser();
        formatter = new HelpFormatter();

        try {
            cmd = parser.parse(options, args);

            if (!isMandatoryArguments()){
                System.err.println("-i [Input File] and -p [private key] arguments are mandatory.\n");
                formatter.printHelp("FermatPreMiningDistributor", options);
                System.exit(-1);
            }

        } catch (ParseException e) {
            formatter.printHelp("FermatPreMiningDistributor", options);
            System.exit(-1);
        }

        //change the loggin level if passed as a parameter
        if (cmd.hasOption("d"))
            logger.setLevel(Level.DEBUG);
        else
            logger.setLevel(Level.ERROR);

        //specify if this is a test execution to avoid controls
        if (cmd.hasOption("t"))
            isTestExecution = true;

        // if is help show help and exit
        if (cmd.hasOption("h")){
            formatter.printHelp("Help", options);
            System.exit(0);
        }

        //network type
        defineNetwork();

        //input file validation
        inputFile =  new File(cmd.getOptionValue("i"));
        if (!inputFile.exists()){
            formatter.printHelp("Input file does not exists.", options);
            System.exit(1);
        }


        /**
         * if the user requested only to generate the Redeem Scripts I will set the passed value as EpochTime and continue.
         * else, I will set current epoch time as value.
         */
        try {
            if (cmd.hasOption("g")){
                String value = cmd.getOptionValue("g");
                EPOC_TIME = Long.parseLong(value);
            }
            else
                EPOC_TIME = System.currentTimeMillis();
        } catch (Exception e){
            System.err.println("There was an error trying to parse the provided EPOC_TIME");
            System.exit(-1);
        }

        //transaction generation
        try {
            fermatTransactionBuilder = new FermatTransactionBuilder(inputFile);
            fermatTransactionBuilder.generateTransactions();
        } catch (AddressFormatException addressFormat) {
          System.err.println("Provided Address on input file is not valid.");
            System.exit(-1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        // if we are generating the scripts. print it on script and exit
        try{
            if (cmd.hasOption("g")){
                generateRedeemScripts();
                System.exit(0);
            }
        } catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }


        //gets the specified private key
        privateKey = cmd.getOptionValue("p");
        if (!isPrivateKeyValid(privateKey)){
            System.err.println("The specified private key " + privateKey + " is not valid on network " + networkParameters.getPaymentProtocolId());
            System.exit(-1);
        }


        /**
         * show transaction summary and wait for user confirmation.
         */
        FermatTransactionsSummary summary = new FermatTransactionsSummary(fermatTransactionBuilder.getFermatTransactions());
        summary.calculateSummary();
        System.out.println();
        System.out.println(summary.toString());
        System.out.println();
        waitForResponse();

        ExecutionLogger executionLogger = null;
        TransactionBuilder transactionBuilder=null;
        try {
            final FermatNetwork network = new FermatNetwork(privateKey); //new FermatNetwork("8PAJoDzv1zMFNmRSG8w6enFGnG9twzRJoPc68hgKBNmNuRktXqv9");
            network.initialize();

            // do we have the funds to send what we are trying to send?
            if (fermatTransactionBuilder.getTotalFermats() > network.getFermatWallet().getBalance(Wallet.BalanceType.AVAILABLE).getValue())
                throw new TransactionErrorException("The amount of tokens to send is higher than our current balance. " + Coin.valueOf(fermatTransactionBuilder.getTotalFermats()).toFriendlyString() + "/" + Coin.valueOf(network.getFermatWallet().getBalance(Wallet.BalanceType.AVAILABLE).getValue()).toFriendlyString());

            // create bitcoin transaction
            transactionBuilder = new TransactionBuilder(network.getGenesisTransaction().getHash(), network.getFermatWallet(), fermatTransactionBuilder.getFermatTransactions());
            for (FermatTransaction fermatTransaction : fermatTransactionBuilder.getFermatTransactions()){
                transactionBuilder.addFermatTransaction(fermatTransaction);
            }

            // completes the transaction
            transactionBuilder.completeTransaction();

            //show balances and wait for confirmation
            Coin currentBalance = network.getFermatWallet().getBalance(Wallet.BalanceType.AVAILABLE);
            System.out.println("Current Premine balance: " + currentBalance.toFriendlyString());
            System.out.println("Confirm that you want to send: " + Coin.valueOf(fermatTransactionBuilder.getTotalFermats()).toFriendlyString());
            waitForResponse();
            network.broadcast(transactionBuilder.getTransaction());

            //log Execution On File
            executionLogger = new ExecutionLogger(fermatTransactionBuilder, transactionBuilder.getTransaction());
            executionLogger.saveOutput();
            System.out.println("Execution output stored at " + executionLogger.getOutputFile().toString());
        } catch (IOException ioexception){
            System.err.println("There was an error saving this execution on file but the transaction was already broadcasted.\nStore this information!\n");
            System.err.println(executionLogger.getOutput());
            System.exit(-1);
        }catch (Exception e) {
            e.printStackTrace();
            if (transactionBuilder != null)
                System.err.println(transactionBuilder.getTransaction().toString());
            System.exit(-1);
        }

        //good bye
        System.exit(0);
    }

    private static ECKey getPrivateKey(String value) throws AddressFormatException {
        return new DumpedPrivateKey(networkParameters, value).getKey();
    }

    /**
     * parse mandatory parameters
     * @return
     */
    private static boolean isMandatoryArguments() {
        if (cmd.hasOption("h"))
            return true;

        if (cmd.hasOption("g")){
            if (cmd.hasOption("i"))
                return true;
            else
                return false;
        }


        if (!cmd.hasOption("p") || !cmd.hasOption("i"))
            return false;
        else
            return true;
    }

    /**
     * shows on screen the redeem script needed to redeem the transactions in the future.
     */
    private static void generateRedeemScripts() {
        System.out.println("Redeem Scripts generation");
        System.out.println(System.lineSeparator());
        TransactionBuilder iopBuilder = new TransactionBuilder();

        for (FermatTransaction fermatTransaction : fermatTransactionBuilder.getFermatTransactions()){
            if (fermatTransaction.isTimeContrained()){
                iopBuilder.addTimeConstrainedTransaction(fermatTransaction);
                System.out.println(fermatTransaction.toString() + " - " + fermatTransaction.getRedeemScript() + " (" + Hex.toHexString(fermatTransaction.getRedeemScript().getProgram()) + ")");
            }
        }
    }

    /**
     * validates if the passed key is a correct private key
     * @param privateKey the string private key to validate.
     * @return true if is valid on the current network
     */
    private static boolean isPrivateKeyValid(String privateKey) {
        try {
            DumpedPrivateKey dumpedPrivateKey = new DumpedPrivateKey(networkParameters, privateKey);
            ECKey key = dumpedPrivateKey.getKey();
            if (key.isPubKeyOnly())
                return false;
        } catch (AddressFormatException e) {
            return false;
        }
        return true;
    }

    private static void defineNetwork() {
        if (cmd.hasOption("n")){
            switch (cmd.getOptionValue("n").toUpperCase()){
                case "MAIN":
                    networkParameters = MainNetParams.get();
                    break;
                case "TEST":
                    networkParameters = TestNet3Params.get();
                    break;
                case "REGTEST":
                    networkParameters = RegTestParams.get();
                    break;
                default:
                    formatter.printHelp("Invalid Network parameter specified.", options);

                    System.exit(1);
                    return;
            }
        } else
            networkParameters = MainNetParams.get();

    }

    private static void waitForResponse(){
        Console c = System.console();
        if (c != null) {
            // printf-like arguments
            c.format("\nPress ENTER if you want to broadcast the transaction. Press Ctrl+C to cancel.\n");
            c.readLine();
        }
    }

    /**
     * returns the epoc time of the start of the process
     * @return
     */
    public static long getEpocTime(){
        return EPOC_TIME;
    }

    /**
     * returns the flag that indicates if this is a test execution
     * @return
     */
    public static boolean isIsTestExecution(){
        return isTestExecution;
    }

    /**
     * adds all the options
     * @return
     */
    private static Options defineOptions(){
        Options options = new Options();
        Option optPrivKey = new Option("p", "privateKey", true, "Private Key for PreMined Transaction funds.");
        optPrivKey.setRequired(false);
        options.addOption(optPrivKey);

        Option optInputFile = new Option("i", "input", true, ".cvs input file to generate fermat transaction.");
        optInputFile.setRequired(false);
        options.addOption(optInputFile);

        Option optNetwork = new Option("n", "network", true, "Fermat Network to connecto to: MAIN, TEST or REGTEST. Default is MAIN.");
        optNetwork.setRequired(false);
        options.addOption(optNetwork);

        Option optHelp = new Option("h", "help", false, "shows this Help");
        optHelp.setRequired(false);
        options.addOption(optHelp);


        Option optDebug = new Option("d", "debug", false, "shows debug information");
        optDebug.setRequired(false);
        options.addOption(optDebug);


        Option optGenerateRedeem = new Option("g", "generate", true, "Generates the Redeem Script and exists. Generation Epoch Time must be provided. ");
        optGenerateRedeem.setRequired(false);
        options.addOption(optGenerateRedeem);

        Option optTest = new Option("t", "test", false, "Reduces the controls needed for a test environment.");
        optTest.setRequired(false);
        options.addOption(optTest);

        return options;

    }

}
