package uk.co.agware.carpet.change.tasks

/**
 * Created by Simon on 29/12/2016.
 */
interface Task : Comparable<Task> {
    var taskName: String
    var taskOrder: Int
    fun performTask(): Boolean

    @Override
    override fun compareTo(o: Task): Int {
        return this.taskOrder.compareTo(o.taskOrder)
    }
}