package uk.co.agware.carpet.change.tasks;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 14/06/2016.
 */
public interface Task extends Comparable<Task> {

    String getTaskName();

    int getTaskOrder();

    boolean performTask();
}
