package com.ok.commitulysses

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.panel
import java.awt.Dimension
import java.awt.Font
import javax.swing.JComponent

class IncompleteChecklistDialog(
    project: Project,
    private val uncheckedItems: List<ChecklistItem>
) : DialogWrapper(project, false) {

    init {
        title = "Commit Blocked"
        setOKButtonText("Got it")
        isOKActionEnabled = true
        init()
    }

    override fun createActions() = arrayOf(okAction)

    override fun createCenterPanel(): JComponent {
        val grouped = uncheckedItems.groupBy { it.category }

        val dialogPanel = panel {
            row {
                icon(AllIcons.General.BalloonWarning)
                label("The following items still need to be checked")
                    .applyToComponent { font = font.deriveFont(Font.BOLD, 14f) }
            }
            row {
                comment("Please review each item and commit again once everything is confirmed.")
            }

            grouped.forEach { (category, categoryItems) ->
                group(category) {
                    categoryItems.forEach { item ->
                        row {
                            icon(ChecklistIcon.fromName(item.iconName).icon)
                            label(item.text)
                                .applyToComponent {
                                    foreground = JBColor.namedColor("Label.errorForeground", JBColor.RED)
                                }
                        }
                    }
                }
            }
        }

        dialogPanel.preferredSize = Dimension(460, dialogPanel.preferredSize.height)
        return dialogPanel
    }
}