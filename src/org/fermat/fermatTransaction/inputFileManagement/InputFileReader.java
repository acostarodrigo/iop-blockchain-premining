package org.fermat.fermatTransaction.inputFileManagement;

import com.google.common.base.Preconditions;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Created by rodrigo on 7/28/16.
 */
public class InputFileReader {
    final private File inputFile;
    List<String> header;
    List<List<String>> rows;

    /**
     * constructor
     * @param inputFile
     */
    public InputFileReader(File inputFile) {
        Preconditions.checkNotNull(inputFile);
        Preconditions.checkArgument(inputFile.exists(), new FileNotFoundException("The specified file does not exists."));

        this.inputFile = inputFile;

        //initialize clasess
        header = new ArrayList<>();
        rows = new ArrayList<>();
    }

    public void read() throws FileNotFoundException {
        List<String> row = new ArrayList<>();
        Scanner scanner = new Scanner(inputFile);
        while (scanner.hasNext()){
            row =  Arrays.asList(scanner.nextLine().split(","));
            rows.add(removeSpaces(row));
        }

        header = prepareHeader(rows.get(0));

        //remove header from rows collection
        rows.remove(0);
        scanner.close();
    }

    public List<String> getHeader() {
        return header;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    /**
     * fixes header with first letter upper case
     * @param rows
     * @return
     */
    private List<String> prepareHeader(List<String> rows){
        List<String> fixedRows = new ArrayList<>();
        for (String column : rows){
            fixedRows.add(column.substring(0, 1).toUpperCase() + column.substring(1).toLowerCase());
        }

        return fixedRows;
    }

    /**
     * removes empty spaces for columns other than name, or index 0
     * @param listToFix
     * @return
     */
    private List<String> removeSpaces(List<String> listToFix){
        List<String> correctedList = new ArrayList<>();
        correctedList.add(listToFix.get(0));
        for (int i=1; i<listToFix.size(); i++){
            correctedList.add(listToFix.get(i).replace(" ", ""));
        }

        return correctedList;
    }
}
