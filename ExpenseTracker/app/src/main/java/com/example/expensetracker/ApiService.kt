package com.example.expensetracker

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://script.google.com/"
private const val PATH = "macros/s/AKfycbwo5z_rBCNL7_HIq8zMRcn8uNXNdPNxtTQZ-7w1s2o4bW5cU8Sm9nfUoxKjCZbk_nXi/exec"

interface ApiService {
    @GET(PATH) suspend fun list(@Query("action") action: String = "list"): String
    @FormUrlEncoded @POST(PATH) suspend fun add(
        @Field("action") action: String = "add", @Field("book") book: String,
        @Field("amount") amount: String, @Field("type") type: String,
        @Field("category") category: String, @Field("paymentMethod") paymentMethod: String,
        @Field("notes") notes: String
    ): String
}

class TransactionRepository {
    private val api = Retrofit.Builder().baseUrl(BASE_URL)
        .client(OkHttpClient.Builder().followRedirects(true).followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build())
        .addConverterFactory(ScalarsConverterFactory.create()).build().create(ApiService::class.java)

    suspend fun list(): Pair<List<String>, List<Transaction>> {
        val root = JsonParser.parseString(api.list())
        if (root.isJsonObject && root.asJsonObject.string("status").equals("error", true))
            error(root.asJsonObject.string("message").ifBlank { "Server error" })
        val obj = if (root.isJsonObject) root.asJsonObject else JsonObject()
        val items: JsonArray = when {
            root.isJsonArray -> root.asJsonArray
            obj.get("transactions")?.isJsonArray == true -> obj.getAsJsonArray("transactions")
            obj.get("data")?.isJsonArray == true -> obj.getAsJsonArray("data")
            else -> error("Expected a JSON transactions array from Apps Script")
        }
        val transactions = items.mapNotNull { element ->
            if (!element.isJsonObject) null else element.asJsonObject.let { row ->
                Transaction(row.string("id"), row.string("book").ifBlank { "Personal" },
                    row.string("amount").toDoubleOrNull() ?: 0.0,
                    row.string("type").ifBlank { "Expense" }, row.string("category"),
                    row.string("paymentMethod").ifBlank { row.string("paymentMode") },
                    row.string("notes"), row.string("date").ifBlank { row.string("timestamp") })
            }
        }
        val books = if (obj.get("books")?.isJsonArray == true) obj.getAsJsonArray("books")
            .mapNotNull { if (it.isJsonPrimitive) it.asString else null } else emptyList()
        return books to transactions
    }

    suspend fun add(transaction: Transaction) {
        val root = JsonParser.parseString(api.add(book = transaction.book,
            amount = transaction.amount.toString(), type = transaction.type,
            category = transaction.category, paymentMethod = transaction.paymentMethod,
            notes = transaction.notes))
        if (!root.isJsonObject) error("Expected a JSON confirmation from Apps Script")
        val obj = root.asJsonObject
        if (obj.string("status").equals("error", true) || obj.get("success")?.toString() == "false")
            error(obj.string("message").ifBlank { "Save failed" })
        if (!obj.string("status").equals("success", true) && obj.get("success")?.toString() != "true")
            error("Apps Script did not confirm the save")
    }
}

private fun JsonObject.string(key: String): String = try {
    val value: JsonElement? = get(key)
    if (value == null || value.isJsonNull) "" else value.asString
} catch (_: Exception) { "" }
