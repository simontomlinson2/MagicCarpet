package uk.co.agware.carpet.change.tasks;

import uk.co.agware.carpet.database.DatabaseConnector;

/**
 * Created by Philip Ward <Philip.Ward@agware.com> on 14/06/2016.
 */
public class ScriptTask extends DatabaseTask {

    private String[] inputList;

    public ScriptTask(DatabaseConnector databaseConnector, String taskName, int taskOrder, String script, String delimiter) {
        super(databaseConnector, taskName, taskOrder);
        delimiter = delimiter == null || "".equals(delimiter) ? ";" : delimiter;
        this.inputList = script.split(delimiter);
    }


    @Override
    public boolean performTask() {
        for(String s : inputList){
            if(!databaseConnector.executeStatement(s.trim())){
                return false;
            }
        }
        return true;
    }
}
