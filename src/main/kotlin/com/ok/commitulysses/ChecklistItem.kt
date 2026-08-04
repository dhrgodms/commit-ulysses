package com.ok.commitulysses

data class ChecklistItem(
    var id: String = "",
    var text: String = "",
    var category: String = "General",
    var iconName: String = ChecklistIcon.CHECK.name,
    var checked: Boolean = false
) {
    // id로만 동등성을 판단: checked가 바뀌면 hashCode도 바뀌어
    // CheckBoxList의 해시 기반 조회(BidirectionalMap)가 깨지는 것을 방지
    override fun equals(other: Any?): Boolean = this === other || (other is ChecklistItem && id == other.id)
    override fun hashCode(): Int = id.hashCode()
}