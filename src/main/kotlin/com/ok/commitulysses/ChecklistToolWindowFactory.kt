package com.ok.commitulysses

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.CheckBoxList
import com.intellij.ui.SeparatorWithText
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.util.UUID
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ChecklistToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(createPanel(project, toolWindow), "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createPanel(project: Project, toolWindow: ToolWindow): JPanel {
        val service = ChecklistSettingsService.getInstance(project)

        val checkBoxList = CategorizedCheckBoxList()

        fun refreshList() {
            checkBoxList.clear()
            service.getItems()
                .groupBy { it.category }
                .values
                .flatten()
                .forEach { item ->
                    checkBoxList.addItem(item, item.text, item.checked)
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

// Assumes items are added pre-grouped by category (see refreshList) so a category change marks a group boundary.
private class CategorizedCheckBoxList : CheckBoxList<ChecklistItem>() {

    // adjustRendering wraps the checkbox in extra icon/indent panels, so the default
    // insets-only hit-testing no longer lines up; delegate to the platform's variant
    // that walks the actual rendered component tree to find the checkbox.
    override fun findPointRelativeToCheckBox(x: Int, y: Int, checkBox: JCheckBox, index: Int): java.awt.Point? =
        findPointRelativeToCheckBoxWithAdjustedRendering(x, y, checkBox, index)

    override fun adjustRendering(
        rootComponent: JComponent,
        checkBox: JCheckBox,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ): JComponent {
        val rendered = super.adjustRendering(rootComponent, checkBox, index, selected, hasFocus)
        val item = getItemAt(index) ?: return rendered
        val previousCategory = if (index > 0) getItemAt(index - 1)?.category else null

        val iconLabel = JLabel(ChecklistIcon.fromName(item.iconName).icon).apply {
            isOpaque = true
            background = checkBox.background
            border = JBUI.Borders.emptyRight(4)
        }
        val rowWithIcon = JPanel(BorderLayout()).apply {
            isOpaque = true
            background = checkBox.background
            add(iconLabel, BorderLayout.WEST)
            add(rendered, BorderLayout.CENTER)
        }

        val indentedRow = JPanel(BorderLayout()).apply {
            isOpaque = true
            background = checkBox.background
            border = JBUI.Borders.emptyLeft(16)
            add(rowWithIcon, BorderLayout.CENTER)
        }

        if (item.category == previousCategory) {
            return indentedRow
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = checkBox.background
            add(createCategoryHeader(item.category), BorderLayout.NORTH)
            add(indentedRow, BorderLayout.CENTER)
        }
    }

    private fun createCategoryHeader(category: String): JComponent =
        SeparatorWithText().apply {
            caption = category
            border = JBUI.Borders.empty(6, 4, 2, 4)
        }
}
