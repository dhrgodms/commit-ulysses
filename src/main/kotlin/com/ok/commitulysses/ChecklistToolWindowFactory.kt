package com.ok.commitulysses

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
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
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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
            .setEditAction {
                val index = checkBoxList.selectedIndex
                val item = if (index != -1) checkBoxList.getItemAt(index) else null
                if (item != null) {
                    val dialog = AddChecklistItemDialog(project, item)
                    if (dialog.showAndGet()) {
                        item.text = dialog.itemText
                        item.category = dialog.selectedCategory
                        item.iconName = dialog.selectedIcon.name
                        refreshList()
                    }
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
            .addExtraAction(object : AnAction("Uncheck All", null, AllIcons.Actions.Unselectall) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

                override fun actionPerformed(e: AnActionEvent) {
                    service.resetAllChecked()
                    refreshList()
                }
            })
            .createPanel()

        return JPanel(BorderLayout()).apply {
            add(decoratedPanel, BorderLayout.CENTER)
        }
    }
}

// Assumes items are added pre-grouped by category (see refreshList) so a category change marks a group boundary.
private class CategorizedCheckBoxList : CheckBoxList<ChecklistItem>() {

    init {
        // Category header isn't part of the real component tree, so lay it out on demand to hit-test the toggle link.
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val index = locationToIndex(e.point)
                if (index == -1) return
                val cellBounds = getCellBounds(index, index) ?: return
                if (!cellBounds.contains(e.point)) return
                val item = getItemAt(index) ?: return
                if (!isGroupStart(item, index)) return

                val header = createCategoryHeader(item.category)
                header.setBounds(0, 0, cellBounds.width, header.preferredSize.height)
                header.doLayout()

                val link = header.getComponent(1)
                if (link.bounds.contains(e.x - cellBounds.x, e.y - cellBounds.y)) {
                    toggleCategory(item.category)
                }
            }
        })
    }

    // Platform's adjusted-rendering hit-test never lays out the rebuilt row, so compute the checkbox's offset ourselves instead.
    override fun findPointRelativeToCheckBox(x: Int, y: Int, checkBox: JCheckBox, index: Int): java.awt.Point? {
        val item = getItemAt(index) ?: return super.findPointRelativeToCheckBox(x, y, checkBox, index)
        val headerHeight = if (isGroupStart(item, index)) createCategoryHeader(item.category).preferredSize.height else 0
        val iconWidth = createIconLabel(item).preferredSize.width
        return super.findPointRelativeToCheckBox(x - INDENT - iconWidth, y - headerHeight, checkBox, index)
    }

    override fun adjustRendering(
        rootComponent: JComponent,
        checkBox: JCheckBox,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ): JComponent {
        val rendered = super.adjustRendering(rootComponent, checkBox, index, selected, hasFocus)
        val item = getItemAt(index) ?: return rendered

        val iconLabel = createIconLabel(item).apply {
            isOpaque = true
            background = checkBox.background
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
            border = JBUI.Borders.emptyLeft(INDENT)
            add(rowWithIcon, BorderLayout.CENTER)
        }

        if (!isGroupStart(item, index)) {
            return indentedRow
        }

        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = checkBox.background
            add(createCategoryHeader(item.category), BorderLayout.NORTH)
            add(indentedRow, BorderLayout.CENTER)
        }
    }

    private fun isGroupStart(item: ChecklistItem, index: Int): Boolean {
        val previousCategory = if (index > 0) getItemAt(index - 1)?.category else null
        return item.category != previousCategory
    }

    private fun createIconLabel(item: ChecklistItem): JLabel =
        JLabel(ChecklistIcon.fromName(item.iconName).icon).apply {
            border = JBUI.Borders.emptyRight(4)
        }

    // Second child (BorderLayout.EAST) is relied on by the click handler above to hit-test the toggle checkbox.
    private fun createCategoryHeader(category: String): JPanel =
        JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(6, 4, 2, 4)
            add(SeparatorWithText().apply { caption = category }, BorderLayout.CENTER)
            add(createCategoryToggleCheckBox(category), BorderLayout.EAST)
        }

    // Fixed-width checkbox instead of a "Select All"/"Deselect All" link so the label length change doesn't jitter the header layout.
    private fun createCategoryToggleCheckBox(category: String): JCheckBox =
        JCheckBox().apply {
            isOpaque = false
            isSelected = isCategoryFullyChecked(category)
            toolTipText = if (isSelected) "Deselect all in \"$category\"" else "Select all in \"$category\""
        }

    private fun isCategoryFullyChecked(category: String): Boolean {
        val items = getAllItems().filter { it.category == category }
        return items.isNotEmpty() && items.all { isItemSelected(it) }
    }

    private fun toggleCategory(category: String) {
        val newState = !isCategoryFullyChecked(category)
        getAllItems().filter { it.category == category }.forEach { item ->
            setItemSelected(item, newState)
            item.checked = newState
        }
        repaint()
    }

    companion object {
        private const val INDENT = 16
    }
}
