package com.ok.beforecommit

import com.intellij.icons.AllIcons
import javax.swing.Icon

enum class ChecklistIcon(val label: String, val icon: Icon) {
    CHECK("Check", AllIcons.General.InspectionsOK),
    WARNING("Warning", AllIcons.General.Warning),
    BUG("Bug", AllIcons.General.Error),
    TODO("Todo", AllIcons.General.TodoDefault),
    QUESTION("Question", AllIcons.General.ContextHelp),
    INFO("Info", AllIcons.General.Information);

    companion object {
        fun fromName(name: String): ChecklistIcon =
            entries.find { it.name == name } ?: CHECK
    }
}