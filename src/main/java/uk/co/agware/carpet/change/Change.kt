package uk.co.agware.carpet.change;

import uk.co.agware.carpet.change.tasks.Task


/**
 * Created by Simon on 29/12/2016.
 */
//TODO Ensure the version String follows the correct format of NUMBER.NUMBER.NUMBER...
//TODO Ensure the taskOrder string is either "" or an integer

class Change(version: String, tasks: List<Task>?) : Comparable<Change> {

    val version = version
    val tasks = tasks

    @Override
    override fun compareTo(o: Change): Int {
        val thisVersionValue = buildVersionValue(version)
        val oVersionValue = buildVersionValue(o.version)
        return thisVersionValue.compareTo(oVersionValue)
    }

    fun buildVersionValue(version: String): Double{
        val versionSplit = version.split(".")
        var value = 0.0
        versionSplit.forEachIndexed { index, v -> value += v.toDouble() / Math.pow(10.0, index.toDouble()) }
        return value
    }

    fun equals(o: Change?): Boolean {
        if (this == o) return true
        if (o !is Change) return false

        val change: Change = o

        return if(version != null) version == change.version else false

    }

    @Override
    override fun hashCode(): Int {
        return if(version != null) version.hashCode() else 0;
    }

    @Override
    override fun toString(): String {
        val  sb = StringBuilder("Change{")
        sb.append("version='").append(version).append('\'')
        sb.append(", tasks=").append(tasks)
        sb.append('}')
        return sb.toString()
    }
}
