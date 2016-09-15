# Fermat Premining Distributor

Distributes the IoP  Premined tokens with time constraint transactions.

### Usage

Execute .jar app with -h for help on the parameters
```
$ java -jar FermatPreMiningDistributor/out/artifacts/FermatPreMiningDistributor_jar/FermatPreMiningDistributor.jar -h
```


```
usage: Help
 -d,--debug              shows debug information
 -g,--generate <arg>     Generates the Redeem Script and exists.
                         Generation Epoch Time must be provided.
 -h,--help               shows this Help
 -i,--input <arg>        .cvs input file to generate fermat transaction.
 -n,--network <arg>      Fermat Network to connecto to: MAIN, TEST or
                         REGTEST. Default is MAIN.
 -p,--privateKey <arg>   Private Key for PreMined Transaction funds.
 -t,--test               Reduces the controls needed for a test
                         environment.


```

Mandatory arguments are:

* **-privateKey**: private key that owns the preMining tokens in the IoP blockchain.
  
* **-input**: Comma separated value (csv) file with the transaction information to be generated.
  
Default network is Mainnet. To switch, use the -network parameter. Example:

```
FermatPreMiningDistributor.jar -i ~/Inputfile.csv -p [validPrivateKey] -n RegTest
```

## Program description

By using a file as an input, it will generate and broadcast an IoP transaction using funds own by the provided private key.

The transaction to be generated will have a time constraint to allow the recievers of the tokens to redeem them only at some point in the future. This transaction uses the OP_CHECKLOCKTIMEVERIFY OP code to force this constraint.

The application also allows the regeneration of the Redeem Script needed to redeem the outputs of the transaction in the future.

By using option **-g** and the **epoch time** as an argument, the exact published redeem script is generated on each execution.

## Input File

The input file is used to read all the transactions that will be generated on a given execution. The expected format will have the following columns as headers:

* Name: an alias of the person recieving the tokens.
* PublicKey: the public key generated by that person.
* Fermats: amoun of tokens to be delivered.
* DaysForPayment: amount of days (0 if none) that will need to pass for this person to be able to redeem the transaction.

Example:

```
Name,PublicKey,Fermats,DaysForPayment
Rodrigo,020a398e58669c6fa1951f39822e1e202c5406485c6fe9ac4fc0e228462e6fb337,10,365
Luis,03ee81b1abc9d7da63a02126bb6f9f1a2adeeec8d3dfee994fe3f2df0689c465e5,5
```

User Rodrigo will recieve 10 tokens and will be able to redeem them in 365 days.
User Luis will recieve 5 tokens redeemable instantly since no time constraint value has been specified.

## Execution

The program perform several validations, allowing broadcasting only when all conditions are met. An example execution could be:

```
FermatPreMiningDistributor.jar -p [ValidPrivateKey] -i ~/testData -n RegTest -t
```

The flag -t (Test) allows the program to reduce the controls and requirements in order to broadcast the transaction.

```
Start time of process (Epoch Time): 1473903112429
Total IoPs to distribute on a single transaction = 15.00 FER
Total distributions included on a single transaction = 2
User Distribution: 
Luis - 5.00 FER
Rodrigo - 10.00 FER


Press ENTER if you want to broadcast the transaction. Press Ctrl+C to cancel.

Connecting to IoP regtest network...
Connected to peer [127.0.0.1]:14877
Current Premine balance: 2099984.99995 FER
Confirm that you want to send: 15.00 FER

Press ENTER if you want to broadcast the transaction. Press Ctrl+C to cancel.

Broadcasting transaction b1f120a5171f852b1e71ddccba372952b2defb1475debd0dc9a5f52565ddb7ff ...
Transaction broadcasted sucessfully
Execution output stored at preMiningDistributor.output
```

The output of the generated information and transactions is stored on the *preMiningDistributor.output* file.


## Authors

* **Rodrigo Acosta** - *Initial work* - [acostarodrigo](https://github.com/acostarodrigo)

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details