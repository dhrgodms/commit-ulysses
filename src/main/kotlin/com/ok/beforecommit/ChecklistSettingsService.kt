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

    fun addItem(text: String) {
        myState.items.add(ChecklistItem(UUID.randomUUID().toString(), text))
    }

    fun removeItem(id: String) {
        myState.items.removeIf { it.id == id }
    }

    fun getCategories(): List<String> = myState.items.map { it.category }.distinct().ifEmpty { listOf("General") }

    companion object {
        fun getInstance(project: Project): ChecklistSettingsService =
            project.getService(ChecklistSettingsService::class.java)
    }
}