package uk.co.agware.carpet.change;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 26/02/2016.
 */
public class Change implements Comparable<Change> {

    private String id;
    private String delimiter;
    private String[] inputList;
    private boolean error;
    private String errorMessage;

    public Change(String id, String rawInput) {
        this.id = id;
        this.delimiter = ";";
        this.inputList = rawInput.split(delimiter);
        for (int i = 0; i < inputList.length; i++) {
            inputList[i] = inputList[i].trim();
        }
    }

    public Change(String id, String rawInput, String delimiter) {
        this.id = id;
        this.delimiter = delimiter;
        this.inputList = rawInput.split(Pattern.quote(delimiter));
    }

    public String getId() {
        return id;
    }

    public String[] getInputList() {
        return inputList;
    }

    public boolean isError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public int compareTo(Change o) {
        String[] thisVersionSplit = id.split("\\.");
        String[] oVersionSplit = o.getId().split("\\.");
        Double thisVersionValue = buildVersionValue(thisVersionSplit);
        Double oVersionValue = buildVersionValue(oVersionSplit);
        if(thisVersionValue.equals(oVersionValue)){
            error = true;
            errorMessage = "Version number " +id +" detected twice";
        }
        return thisVersionValue.compareTo(oVersionValue);
    }

    private double buildVersionValue(String[] numbers){
        double value = 0.0;
        for (int i = 0; i < numbers.length; i++) {
            value += Double.parseDouble(numbers[i]) / Math.pow(10, i);
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Change)) return false;

        Change change = (Change) o;

        return id != null ? id.equals(change.id) : change.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Change{" +
                "id='" + id + '\'' +
                ", delimiter='" + delimiter + '\'' +
                ", inputList=" + Arrays.toString(inputList) +
                ", error=" + error +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
