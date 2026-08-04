package com.ok.commitulysses

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.CheckBoxList
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.util.UUID
import javax.swing.JPanel

class ChecklistToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(createPanel(project, toolWindow), "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createPanel(project: Project, toolWindow: ToolWindow): JPanel {
        val service = ChecklistSettingsService.getInstance(project)

        val checkBoxList = CheckBoxList<ChecklistItem>()

        fun refreshList() {
            checkBoxList.clear()
            service.getItems().forEach { item ->
                checkBoxList.addItem(item, "[${item.category}] ${item.text}", item.checked)
            }
        }
        refreshList()

        checkBoxList.setCheckBoxListListener { index, checked ->
            checkBoxList.getItemAt(index)?.checked = checked
        }

        project.messageBus.connect(toolWindow.disposable).subscribe(
            ChecklistResetListener.TOPIC,
            ChecklistResetListener {
                ApplicationManager.getApplication().invokeLater { refreshList() }
            }
        )

        val decoratedPanel = ToolbarDecorator.createDecorator(checkBoxList)
            .setAddAction {
                val dialog = AddChecklistItemDialog(project)
                if (dialog.showAndGet()) {
                    val item = ChecklistItem(
                        id = UUID.randomUUID().toString(),
                        text = dialog.itemText,
                        category = dialog.selectedCategory,
                        iconName = dialog.selectedIcon.name
                    )
                    service.getItems().add(item)
                    refreshList()
                }
            }
            .setRemoveAction {
                val index = checkBoxList.selectedIndex
                if (index != -1) {
                    val item = checkBoxList.getItemAt(index)
                    if (item != null) {
                        service.getItems().removeIf { it.id == item.id }
                        refreshList()
                    }
                }
            }
            .createPanel()

        return JPanel(BorderLayout()).apply {
            add(decoratedPanel, BorderLayout.CENTER)
        }
    }
}
