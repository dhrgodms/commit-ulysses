package com.ok.beforecommit

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import java.util.*

@Service(Service.Level.PROJECT)
@State(name = "CommitChecklistSettings", storages = [Storage("commitChecklist.xml")])
class ChecklistSettingsService : PersistentStateComponent<ChecklistSettingsService.State> {

    class State {
        var items: MutableList<ChecklistItem> = mutableListOf()
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun getItems(): MutableList<ChecklistItem> = myState.items

    fun addItem(text: String, category: String, iconName: String) {
        myState.items.add(ChecklistItem(UUID.randomUUID().toString(), text, category, iconName))
    }

    fun removeItem(id: String) {
        myState.items.removeIf { it.id == id }
    }

    /** 기본 카테고리 + 실제 등록된 카테고리를 중복 없이 합쳐서 반환 */
    fun getCategories(): List<String> {
        val used = myState.items.map { it.category }
        return (ChecklistCategories.DEFAULTS + used).distinct()
    }

    companion object {
        fun getInstance(project: Project): ChecklistSettingsService =
            project.getService(ChecklistSettingsService::class.java)
    }
}