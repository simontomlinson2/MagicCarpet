package uk.co.agware.carpet.change.tasks;

import uk.co.agware.carpet.database.DatabaseConnector;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 14/06/2016.
 */
public abstract class DatabaseTask implements Task {

    protected DatabaseConnector databaseConnector;
    private String taskName;
    private int taskOrder;

    public DatabaseTask(DatabaseConnector databaseConnector, String taskName, int taskOrder) {
        this.databaseConnector = databaseConnector;
        this.taskName = taskName;
        this.taskOrder = taskOrder;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public int getTaskOrder() {
        return taskOrder;
    }

    @Override
    public int compareTo(Task o) {
        return Integer.compare(taskOrder, o.getTaskOrder());
    }
}