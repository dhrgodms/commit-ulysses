package com.ok.commitulysses

data class ChecklistItem(
    var id: String = "",
    var text: String = "",
    var category: String = "General",
    var iconName: String = ChecklistIcon.CHECK.name
)