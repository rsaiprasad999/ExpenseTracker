package com.example.expensetracker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.UUID

class ExpenseViewModel : ViewModel() {
    private val repository = TransactionRepository()
    var state by mutableStateOf(DashboardState()); private set
    init { refresh() }
    fun selectBook(book: String) { state = state.copy(selectedBook = book) }
    fun clearMessage() { state = state.copy(message = null) }
    fun refresh() {
        viewModelScope.launch {
            state = state.copy(loading = true)
            try {
                val (remoteBooks, transactions) = repository.list()
                val books = (state.books + remoteBooks + transactions.map { it.book }).distinct()
                state = state.copy(loading = false, books = books, transactions = transactions)
            } catch (e: Exception) {
                state = state.copy(loading = false, message = "Refresh failed: ${e.message}")
            }
        }
    }
    fun add(amount: Double, type: String, category: String, method: String, notes: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            state = state.copy(submitting = true)
            try {
                repository.add(Transaction(UUID.randomUUID().toString(), state.selectedBook, amount,
                    type, category, method, notes, ""))
                state = state.copy(submitting = false, message = "Transaction saved")
                onSaved()
                refresh()
            } catch (e: Exception) {
                state = state.copy(submitting = false, message = "Save failed: ${e.message}")
            }
        }
    }
}
