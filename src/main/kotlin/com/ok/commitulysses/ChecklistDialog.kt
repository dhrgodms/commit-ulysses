package com.ok.commitulysses

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.awt.Dimension
import javax.swing.JComponent

class ChecklistDialog(
    project: Project,
    private val items: List<ChecklistItem>
) : DialogWrapper(project) {

    private val itemCheckboxPairs = mutableListOf<Pair<ChecklistItem, JBCheckBox>>()
    private val categoryCheckboxes = mutableMapOf<String, MutableList<JBCheckBox>>()
    private val categoryLinks = mutableMapOf<String, ActionLink>()
    private var selectAllLink: ActionLink? = null
    private val selectAllString = "Select All"
    private val deSelectAllString = "Deselect All"

    init {
        title = "Commit Ulysses"
        setOKButtonText("Commit")
        setCancelButtonText("Cancel")
        init()
    }

    override fun createCenterPanel(): JComponent {
        itemCheckboxPairs.clear()
        categoryCheckboxes.clear()
        categoryLinks.clear()
        val grouped = items.groupBy { it.category }

        val dialogPanel = panel {
            row {
                label("Please confirm the following before committing")
                    .applyToComponent { font = font.deriveFont(font.style or java.awt.Font.BOLD, 14f) }
                    .resizableColumn()
                selectAllLink = link(selectAllString) {}.align(AlignX.RIGHT).component
            }
            row {
                comment("The commit will be blocked until all items are checked.")
            }

            grouped.forEach { (category, categoryItems) ->
                group(category) {
                    row {
                        label("").resizableColumn()
                        categoryLinks[category] = link(selectAllString) {}.align(AlignX.RIGHT).component
                    }
                    categoryItems.forEach { item ->
                        row {
                            icon(ChecklistIcon.fromName(item.iconName).icon)
                            val cb = checkBox(item.text).component
                            cb.isSelected = item.checked
                            cb.addItemListener {
                                item.checked = cb.isSelected
                                updateLinkTexts()
                            }
                            itemCheckboxPairs.add(item to cb)
                            categoryCheckboxes.getOrPut(category) { mutableListOf() }.add(cb)
                        }
                    }
                }
            }
        }

        selectAllLink?.addActionListener {
            val checkAll = itemCheckboxPairs.any { !it.second.isSelected }
            itemCheckboxPairs.forEach { it.second.isSelected = checkAll }
        }
        categoryLinks.forEach { (category, link) ->
            link.addActionListener {
                val checkboxes = categoryCheckboxes[category].orEmpty()
                val checkAll = checkboxes.any { !it.isSelected }
                checkboxes.forEach { it.isSelected = checkAll }
            }
        }
        updateLinkTexts()

        dialogPanel.preferredSize = Dimension(480, dialogPanel.preferredSize.height)
        return dialogPanel
    }

    private fun updateLinkTexts() {
        val allChecked = itemCheckboxPairs.isNotEmpty() && itemCheckboxPairs.all { it.second.isSelected }
        selectAllLink?.text = if (allChecked) deSelectAllString else selectAllString

        categoryCheckboxes.forEach { (category, checkboxes) ->
            val categoryAllChecked = checkboxes.isNotEmpty() && checkboxes.all { it.isSelected }
            categoryLinks[category]?.text = if (categoryAllChecked) deSelectAllString else selectAllString
        }
    }

    override fun getPreferredFocusedComponent(): JComponent? = itemCheckboxPairs.firstOrNull()?.second

    fun allChecked(): Boolean = itemCheckboxPairs.all { it.second.isSelected }

    fun uncheckedItems(): List<ChecklistItem> =
        itemCheckboxPairs.filter { !it.second.isSelected }.map { it.first }
}
