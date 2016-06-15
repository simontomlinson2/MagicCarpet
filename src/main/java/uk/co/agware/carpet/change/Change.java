package uk.co.agware.carpet.change;

import uk.co.agware.carpet.change.tasks.Task;

import java.util.List;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 26/02/2016.
 */
//TODO Ensure the version String follows the correct format of NUMBER.NUMBER.NUMBER...
    //TODO Ensure the taskOrder string is either "" or an integer
public class Change implements Comparable<Change> {

    private String version;
    private List<Task> tasks;

    public Change(String version, List<Task> tasks) {
        this.version = version;
        this.tasks = tasks;
    }

    public String getVersion() {
        return version;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    @Override
    public int compareTo(Change o) {
        double thisVersionValue = buildVersionValue(version);
        double oVersionValue = buildVersionValue(o.version);
        return Double.compare(thisVersionValue, oVersionValue);
    }

    private double buildVersionValue(String version){
        String[] versionSplit = version.split("\\.");
        double value = 0.0;
        for (int i = 0; i < versionSplit.length; i++) {
            value += Double.parseDouble(versionSplit[i]) / Math.pow(10, i);
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Change)) return false;

        Change change = (Change) o;

        return version != null ? version.equals(change.version) : change.version == null;

    }

    @Override
    public int hashCode() {
        return version != null ? version.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Change{");
        sb.append("version='").append(version).append('\'');
        sb.append(", tasks=").append(tasks);
        sb.append('}');
        return sb.toString();
    }
}
