package com.ok.commitulysses

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.util.UUID
import javax.swing.DefaultListModel
import javax.swing.JPanel

class ChecklistToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(createPanel(project), "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createPanel(project: Project): JPanel {
        val service = ChecklistSettingsService.getInstance(project)
        val listModel = DefaultListModel<ChecklistItem>()
        service.getItems().forEach { listModel.addElement(it) }

        val jbList = JBList(listModel)
        jbList.cellRenderer = ChecklistItemRenderer.create()

        val decoratedPanel = ToolbarDecorator.createDecorator(jbList)
            .setAddAction {
                val dialog = AddChecklistItemDialog(project)
                if (dialog.showAndGet()) {
                    val item = ChecklistItem(
                        id = UUID.randomUUID().toString(),
                        text = dialog.itemText,
                        category = dialog.selectedCategory,
                        iconName = dialog.selectedIcon.name
                    )
                    listModel.addElement(item)
                    service.getItems().add(item)
                }
            }
            .setRemoveAction {
                val index = jbList.selectedIndex
                if (index != -1) {
                    val item = listModel.getElementAt(index)
                    listModel.remove(index)
                    service.getItems().removeIf { it.id == item.id }
                }
            }
            .createPanel()

        return JPanel(BorderLayout()).apply {
            add(decoratedPanel, BorderLayout.CENTER)
        }
    }
}
