package com.ok.beforecommit

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.util.PairConsumer

class ChecklistCheckinHandler(private val panel: CheckinProjectPanel) : CheckinHandler() {

    override fun beforeCheckin(
        executor: CommitExecutor?,
        additionalDataConsumer: PairConsumer<Any, Any>?
    ): ReturnResult {
        val project = panel.project
        val items = ChecklistSettingsService.getInstance(project).getItems()

        if (items.isEmpty()) return ReturnResult.COMMIT

        val dialog = ChecklistDialog(project, items)
        if (!dialog.showAndGet()) {
            return ReturnResult.CANCEL
        }

        if (!dialog.allChecked()) {
            Messages.showWarningDialog(
                project,
                "The following items are not checked:\n" + dialog.uncheckedTexts().joinToString("\n") { "- $it" },
                "Commit Checklist"
            )
            return ReturnResult.CANCEL
        }

        return ReturnResult.COMMIT
    }
}