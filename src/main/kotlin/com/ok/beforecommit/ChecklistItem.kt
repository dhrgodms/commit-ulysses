package com.ok.beforecommit

data class ChecklistItem(
    var id: String = "",
    var text: String = "",
    var category: String = "General",
    var iconName: String = ChecklistIcon.CHECK.name
)