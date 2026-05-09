package com.notathermal.app.ai

import org.json.JSONArray
import org.json.JSONObject

data class ParsedInvoiceItem(
    val name: String,
    val quantity: Double,
    val price: Double,
    val discount: Double = 0.0
)

data class ParsedInvoice(
    val items: List<ParsedInvoiceItem>,
    val customerName: String?,
    val cashierName: String?,
    val taxPercent: Double?,
    val notes: String?
)

/**
 * Sends a free-form Indonesian description (typed or transcribed from voice)
 * to Gemini and returns a structured invoice draft. The schema is enforced
 * server-side via `responseSchema`, so the parser only has to translate JSON
 * into our domain model — no fragile regex needed.
 */
class AiInvoiceParser(private val client: GeminiClient = GeminiClient()) {

    suspend fun parse(apiKey: String, userText: String): Result<ParsedInvoice> {
        val schema = buildSchema()
        return client.generateJson(
            apiKey = apiKey,
            systemInstruction = SYSTEM_PROMPT,
            userText = userText,
            responseSchema = schema
        ).mapCatching { json ->
            ParsedInvoice(
                items = json.optJSONArray("items")?.toItems().orEmpty(),
                customerName = json.optString("customer_name").takeIf { it.isNotBlank() },
                cashierName = json.optString("cashier_name").takeIf { it.isNotBlank() },
                taxPercent = json.opt("tax_percent")?.toString()?.toDoubleOrNull(),
                notes = json.optString("notes").takeIf { it.isNotBlank() }
            )
        }
    }

    private fun JSONArray.toItems(): List<ParsedInvoiceItem> {
        val out = ArrayList<ParsedInvoiceItem>(length())
        for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            val name = o.optString("name").trim()
            if (name.isEmpty()) continue
            out += ParsedInvoiceItem(
                name = name,
                quantity = o.opt("quantity")?.toString()?.toDoubleOrNull() ?: 1.0,
                price = o.opt("price")?.toString()?.toDoubleOrNull() ?: 0.0,
                discount = o.opt("discount")?.toString()?.toDoubleOrNull() ?: 0.0
            )
        }
        return out
    }

    private fun buildSchema(): JSONObject {
        val itemSchema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put("name", JSONObject().put("type", "STRING"))
                    .put("quantity", JSONObject().put("type", "NUMBER"))
                    .put("price", JSONObject().put("type", "NUMBER"))
                    .put("discount", JSONObject().put("type", "NUMBER"))
            )
            .put("required", JSONArray().put("name").put("quantity").put("price"))

        return JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put(
                        "items",
                        JSONObject().put("type", "ARRAY").put("items", itemSchema)
                    )
                    .put("customer_name", JSONObject().put("type", "STRING"))
                    .put("cashier_name", JSONObject().put("type", "STRING"))
                    .put("tax_percent", JSONObject().put("type", "NUMBER"))
                    .put("notes", JSONObject().put("type", "STRING"))
            )
            .put("required", JSONArray().put("items"))
    }

    companion object {
        private val SYSTEM_PROMPT = """
            Anda adalah asisten yang membantu kasir Indonesia menyusun invoice/struk
            dari deskripsi bebas (boleh diketik atau hasil transkrip suara).

            Tugas: ekstrak daftar item beserta nama, jumlah (quantity), dan harga
            per satuan (price) dalam Rupiah sebagai angka biasa (tanpa "Rp", titik,
            atau koma sebagai pemisah ribuan). Jika user menulis "5rb" / "5 ribu" /
            "lima ribu" itu artinya 5000. "10jt" = 10000000.

            Aturan harga: yang diisi di "price" adalah harga PER SATUAN, bukan total.
            Contoh: "3 indomie 5000" → quantity 3, price 5000. "2 teh @ 4500" sama.
            "lima nasi total 60rb" → quantity 5, price 12000.

            Aturan kuantitas: kalau tidak disebut, default 1.

            Tambahan opsional yang bisa diekstrak kalau disebut:
            - customer_name: nama pelanggan ("untuk pak budi" → "Pak Budi")
            - cashier_name: nama kasir ("kasir siti" → "Siti")
            - tax_percent: persen pajak ("pajak 10%" → 10)
            - discount per item: kalau disebut diskon item ("indomie diskon 1000")
            - notes: catatan tambahan singkat

            JANGAN tambah item yang tidak disebut user. JANGAN melakukan kalkulasi
            total — hanya isi field per item, total akan dihitung aplikasi.

            Balas HANYA JSON valid sesuai schema. Gunakan Bahasa Indonesia untuk
            nilai teks (mis. nama item & customer).
        """.trimIndent()
    }
}
