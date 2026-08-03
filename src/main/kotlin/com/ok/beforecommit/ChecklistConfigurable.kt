package com.ok.beforecommit

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import java.util.*
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel

class ChecklistConfigurable(private val project: Project) : Configurable {

    private lateinit var listModel: DefaultListModel<ChecklistItem>
    private lateinit var jbList: JBList<ChecklistItem>

    override fun getDisplayName(): String = "Commit Checklist"

    override fun createComponent(): JComponent {
        listModel = DefaultListModel()
        ChecklistSettingsService.getInstance(project).getItems().forEach { listModel.addElement(it) }

        jbList = JBList(listModel)
        jbList.cellRenderer = javax.swing.ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
            javax.swing.JLabel("[${value.category}] ${value.text}").apply {
                isOpaque = true
                if (isSelected) {
                    background = list.selectionBackground
                    foreground = list.selectionForeground
                } else {
                    background = list.background
                    foreground = list.foreground
                }
            }
        }

        val decoratedPanel = ToolbarDecorator.createDecorator(jbList)
            .setAddAction {
                val existingCategories = ChecklistSettingsService.getInstance(project).getCategories()
                val category = Messages.showEditableChooseDialog(
                    "Enter or select a category",
                    "Category",
                    null,
                    existingCategories.toTypedArray(),
                    existingCategories.firstOrNull() ?: "General",
                    null
                ) ?: return@setAddAction

                val text = Messages.showInputDialog(project, "Enter a checklist item", "Add Item", null)
                if (!text.isNullOrBlank()) {
                    listModel.addElement(
                        ChecklistItem(
                            UUID.randomUUID().toString(),
                            text,
                            category.ifBlank { "General" })
                    )
                }
            }
            .setRemoveAction {
                val index = jbList.selectedIndex
                if (index != -1) {
                    listModel.remove(index)
                }
            }
            .createPanel()

        return JPanel(BorderLayout()).apply {
            add(decoratedPanel, BorderLayout.CENTER)
        }
    }

    override fun isModified(): Boolean {
        val current = ChecklistSettingsService.getInstance(project).getItems()
        val edited = currentListItems()
        if (current.size != edited.size) return true
        return current.zip(edited).any { (a, b) -> a.text != b.text || a.category != b.category }
    }

    override fun apply() {
        val service = ChecklistSettingsService.getInstance(project)
        service.getItems().clear()
        service.getItems().addAll(currentListItems())
    }

    private fun currentListItems(): List<ChecklistItem> {
        val result = mutableListOf<ChecklistItem>()
        for (i in 0 until listModel.size()) {
            result.add(listModel.getElementAt(i))
        }
        return result
    }
}