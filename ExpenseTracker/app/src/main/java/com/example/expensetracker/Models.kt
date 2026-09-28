package com.example.expensetracker

data class Transaction(
    val id: String, val book: String, val amount: Double, val type: String,
    val category: String, val paymentMethod: String, val notes: String, val date: String
)
data class DashboardState(
    val books: List<String> = listOf("Personal", "Business", "Savings"),
    val selectedBook: String = "Personal", val transactions: List<Transaction> = emptyList(),
    val loading: Boolean = false, val submitting: Boolean = false, val message: String? = null
)
