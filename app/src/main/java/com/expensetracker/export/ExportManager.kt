package com.expensetracker.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.expensetracker.domain.model.Expense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor() {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun toCsv(expenses: List<Expense>): String {
        val sb = StringBuilder()
        sb.appendLine("Date,Title,Amount,Currency,Paid By,Category,Notes,Split Type")
        expenses.forEach { e ->
            sb.appendLine("${dateFmt.format(Date(e.expenseDate))},\"${escape(e.title)}\",${e.amount},${e.currencyCode},\"${escape(e.paidByName)}\",${e.categoryId},\"${escape(e.notes)}\",${e.splitType}")
        }
        return sb.toString()
    }

    fun toExcel(expenses: List<Expense>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\"?>\n")
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\">\n")
        sb.append("<Worksheet ss:Name=\"Expenses\" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n")
        sb.append("<Table>\n")
        sb.append("<Row><Cell><Data>Date</Data></Cell><Cell><Data>Title</Data></Cell><Cell><Data>Amount</Data></Cell><Cell><Data>Currency</Data></Cell><Cell><Data>Paid By</Data></Cell></Row>\n")
        expenses.forEach { e ->
            sb.append("<Row>")
            sb.append("<Cell><Data>${dateFmt.format(Date(e.expenseDate))}</Data></Cell>")
            sb.append("<Cell><Data>${escapeXml(e.title)}</Data></Cell>")
            sb.append("<Cell><Data>${e.amount}</Data></Cell>")
            sb.append("<Cell><Data>${e.currencyCode}</Data></Cell>")
            sb.append("<Cell><Data>${escapeXml(e.paidByName)}</Data></Cell>")
            sb.append("</Row>\n")
        }
        sb.append("</Table></Worksheet></Workbook>")
        return sb.toString()
    }

    fun toPdf(expenses: List<Expense>): ByteArray {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 12f }
        var y = 40f
        canvas.drawText("ExpenseTracker Pro - Expense Report", 40f, y, titlePaint)
        y += 30f
        canvas.drawText("Generated: ${dateFmt.format(Date())}", 40f, y, bodyPaint)
        y += 30f
        expenses.take(40).forEach { e ->
            canvas.drawText("${dateFmt.format(Date(e.expenseDate))}  ${e.title}  ${e.amount} ${e.currencyCode}", 40f, y, bodyPaint)
            y += 20f
        }
        doc.finishPage(page)
        val stream = java.io.ByteArrayOutputStream()
        doc.writeTo(stream)
        doc.close()
        return stream.toByteArray()
    }

    private fun escape(s: String) = s.replace("\"", "\"\"")
    private fun escapeXml(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
