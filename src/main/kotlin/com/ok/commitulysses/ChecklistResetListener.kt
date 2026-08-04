package com.ok.commitulysses

import com.intellij.util.messages.Topic

fun interface ChecklistResetListener {
    fun onChecklistReset()

    companion object {
        val TOPIC: Topic<ChecklistResetListener> =
            Topic.create("Commit Ulysses Checklist Reset", ChecklistResetListener::class.java)
    }
}
