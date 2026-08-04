package com.ok.commitulysses

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
            IncompleteChecklistDialog(project, dialog.uncheckedItems()).show()
            return ReturnResult.CANCEL
        }

        return ReturnResult.COMMIT
    }

    override fun checkinSuccessful() {
        val project = panel.project
        ChecklistSettingsService.getInstance(project).resetAllChecked()
        project.messageBus.syncPublisher(ChecklistResetListener.TOPIC).onChecklistReset()
    }
}