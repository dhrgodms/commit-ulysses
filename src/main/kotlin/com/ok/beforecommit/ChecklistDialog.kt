package com.ok.beforecommit

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import java.awt.Dimension
import javax.swing.JComponent

class ChecklistDialog(
    project: Project,
    private val items: List<ChecklistItem>
) : DialogWrapper(project) {

    private val checkboxes = mutableListOf<JBCheckBox>()

    init {
        title = "Commit Checklist"
        setOKButtonText("Commit")
        setCancelButtonText("Cancel")
        init()
    }

    override fun createCenterPanel(): JComponent {
        checkboxes.clear()
        val grouped = items.groupBy { it.category }

        val dialogPanel = panel {
            row {
                label("Please confirm the following before committing")
                    .applyToComponent { font = font.deriveFont(font.style or java.awt.Font.BOLD, 14f) }
            }
            row {
                comment("The commit will be blocked until all items are checked.")
            }

            grouped.forEach { (category, categoryItems) ->
                group(category) {
                    categoryItems.forEach { item ->
                        row {
                            icon(ChecklistIcon.fromName(item.iconName).icon)
                            val cb = checkBox(item.text).component
                            checkboxes.add(cb)
                        }
                    }
                }
            }
        }

        dialogPanel.preferredSize = Dimension(480, dialogPanel.preferredSize.height)
        return dialogPanel
    }

    override fun getPreferredFocusedComponent(): JComponent? = checkboxes.firstOrNull()

    fun allChecked(): Boolean = checkboxes.all { it.isSelected }

    fun uncheckedTexts(): List<String> =
        checkboxes.filter { !it.isSelected }.map { it.text }
}