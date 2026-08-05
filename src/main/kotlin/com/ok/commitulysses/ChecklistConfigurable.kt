package com.ok.commitulysses

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.*
import javax.swing.*

class ChecklistConfigurable(private val project: Project) : Configurable {

    private lateinit var listModel: DefaultListModel<ChecklistItem>
    private lateinit var jbList: JBList<ChecklistItem>

    override fun getDisplayName(): String = "Commit Ulysses"

    override fun createComponent(): JComponent {
        listModel = DefaultListModel()
        ChecklistSettingsService.getInstance(project).getItems().forEach { listModel.addElement(it) }

        jbList = JBList(listModel)
        jbList.cellRenderer = ChecklistItemRenderer.create()

        val decoratedPanel = ToolbarDecorator.createDecorator(jbList)
            .setAddAction {
                val dialog = AddChecklistItemDialog(project)
                if (dialog.showAndGet()) {
                    listModel.addElement(
                        ChecklistItem(
                            id = UUID.randomUUID().toString(),
                            text = dialog.itemText,
                            category = dialog.selectedCategory,
                            iconName = dialog.selectedIcon.name
                        )
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
        return current.zip(edited).any { (a, b) ->
            a.text != b.text || a.category != b.category || a.iconName != b.iconName
        }
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

/** 항목 추가/수정용 다이얼로그: 카테고리(선택/생성) + 아이콘 + 텍스트 */
class AddChecklistItemDialog(
    private val project: Project,
    private val existingItem: ChecklistItem? = null
) : DialogWrapper(project) {

    private lateinit var categoryCombo: ComboBox<String>
    private lateinit var newCategoryField: JBTextField
    private lateinit var iconCombo: ComboBox<ChecklistIcon>
    private lateinit var textField: JBTextField

    var selectedCategory: String = existingItem?.category ?: "General"
        private set
    var selectedIcon: ChecklistIcon = existingItem?.let { ChecklistIcon.fromName(it.iconName) } ?: ChecklistIcon.CHECK
        private set
    var itemText: String = existingItem?.text ?: ""
        private set

    init {
        title = if (existingItem != null) "Edit Checklist Item" else "Add Checklist Item"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val categories = ChecklistSettingsService.getInstance(project).getCategories()

        val dialogPanel = panel {
            row("Category:") {
                categoryCombo = comboBox(categories).component
                categoryCombo.selectedItem = selectedCategory
            }
            row("New category:") {
                newCategoryField = textField()
                    .comment("Leave empty to use the category selected above")
                    .component
            }
            row("Icon:") {
                iconCombo = comboBox(
                    ChecklistIcon.entries.toList(),
                    renderer = ListCellRenderer<ChecklistIcon?> { list, value, _, isSelected, _ ->
                        JLabel(value?.label, value?.icon, SwingConstants.LEFT).apply {
                            isOpaque = true
                            if (isSelected) {
                                background = list?.selectionBackground
                                foreground = list?.selectionForeground
                            } else {
                                background = list?.background
                                foreground = list?.foreground
                            }
                        }
                    }
                ).component
                iconCombo.selectedItem = selectedIcon
            }
            row("Item text:") {
                textField = textField()
                    .align(AlignX.FILL)
                    .component
                textField.text = itemText
            }
        }

        dialogPanel.preferredSize = Dimension(420, dialogPanel.preferredSize.height)
        return dialogPanel
    }

    override fun doValidate(): ValidationInfo? {
        if (textField.text.isBlank()) {
            return ValidationInfo("Item text cannot be empty", textField)
        }
        return null
    }

    override fun doOKAction() {
        selectedCategory = newCategoryField.text.takeIf { it.isNotBlank() }
            ?: (categoryCombo.selectedItem as? String ?: "General")
        selectedIcon = iconCombo.selectedItem as? ChecklistIcon ?: ChecklistIcon.CHECK
        itemText = textField.text.trim()
        super.doOKAction()
    }
}
