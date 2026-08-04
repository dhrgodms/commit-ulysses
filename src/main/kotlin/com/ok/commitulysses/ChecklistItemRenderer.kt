package com.ok.commitulysses

import javax.swing.JLabel
import javax.swing.ListCellRenderer
import javax.swing.SwingConstants

object ChecklistItemRenderer {
    fun create(): ListCellRenderer<ChecklistItem> = ListCellRenderer { list, value, _, isSelected, _ ->
        JLabel(
            "[${value.category}] ${value.text}",
            ChecklistIcon.fromName(value.iconName).icon,
            SwingConstants.LEFT
        ).apply {
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
}
