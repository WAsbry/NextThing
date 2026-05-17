package com.nextthing.app.data.export

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.content.FileProvider
import com.nextthing.app.data.local.dao.TaskDao
import com.nextthing.app.data.mapper.toDomain
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskStatus
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导出格式
 */
enum class ExportFormat(
    val displayName: String,
    val extension: String,
    val mimeType: String
) {
    CSV("CSV 表格", "csv", "text/csv"),
    EXCEL("Excel 表格", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    MARKDOWN("Markdown", "md", "text/markdown")
}

/**
 * 任务数据导出器
 *
 * 支持将任务数据按时间范围导出为 CSV/XLSX/Markdown 格式。
 * XLSX 通过直接生成 OOXML (ZIP + XML) 实现，无需 Apache POI 等重量级依赖。
 */
@Singleton
class TaskExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao
) {
    companion object {
        private const val TAG = "TaskExporter"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        private val FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        private val HEADERS = listOf(
            "任务标题", "描述", "分类", "状态", "优先级",
            "截止时间", "创建时间", "完成时间", "标签"
        )
    }

    /**
     * 导出任务数据
     */
    suspend fun export(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        format: ExportFormat
    ): Result<Uri> {
        return try {
            Timber.tag(TAG).d("开始导出: $startDate ~ $endDate, 格式=${format.displayName}")

            val taskEntities = taskDao.getTasksByDateRangeOnce(startDate, endDate)
            val tasks = taskEntities
                .map { it.toDomain() }
                .filter { !it.isTemplate }
                .sortedBy { it.createdAt }

            if (tasks.isEmpty()) {
                return Result.failure(Exception("该时间范围内没有任务数据"))
            }

            Timber.tag(TAG).d("查询到 ${tasks.size} 个任务")

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT)
            val fileName = "NextThing_${timestamp}.${format.extension}"
            val file = File(exportDir, fileName)

            when (format) {
                ExportFormat.CSV -> exportCsv(tasks, file)
                ExportFormat.EXCEL -> exportXlsx(tasks, file)
                ExportFormat.MARKDOWN -> exportMarkdown(tasks, file, startDate, endDate)
            }

            Timber.tag(TAG).d("✅ 导出成功: ${file.name} (${file.length()} bytes)")

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            Result.success(uri)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 导出失败")
            Result.failure(e)
        }
    }

    // ── CSV 导出 ──

    private fun exportCsv(tasks: List<Task>, file: File) {
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                // UTF-8 BOM，确保 WPS/Excel 正确识别中文
                writer.write("\uFEFF")
                writer.write(HEADERS.joinToString(",") { escapeCsv(it) })
                writer.write("\n")
                tasks.forEach { task ->
                    writer.write(taskToRow(task).joinToString(",") { escapeCsv(it) })
                    writer.write("\n")
                }
            }
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    // ── XLSX 导出（纯 XML + ZIP，无第三方依赖）──

    private fun exportXlsx(tasks: List<Task>, file: File) {
        // 收集所有字符串到共享字符串表（SST），这是 OOXML 的标准做法
        val allStrings = mutableListOf<String>()
        val stringIndex = mutableMapOf<String, Int>()

        fun addString(s: String): Int {
            return stringIndex.getOrPut(s) {
                allStrings.add(s)
                allStrings.size - 1
            }
        }

        // 注册表头
        HEADERS.forEach { addString(it) }
        // 注册所有数据
        val rows = tasks.map { task -> taskToRow(task).map { addString(it) } }

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            // [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="任务数据" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // xl/styles.xml（表头加粗 + 灰底）
            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="等线"/></font>
    <font><b/><sz val="11"/><name val="等线"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFD9D9D9"/></patternFill></fill>
  </fills>
  <borders count="1"><border/></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>""".toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // xl/sharedStrings.xml
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            val sstBuilder = StringBuilder()
            sstBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sstBuilder.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${allStrings.size}" uniqueCount="${allStrings.size}">""")
            allStrings.forEach { s ->
                sstBuilder.append("<si><t>")
                sstBuilder.append(escapeXml(s))
                sstBuilder.append("</t></si>")
            }
            sstBuilder.append("</sst>")
            zip.write(sstBuilder.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // xl/worksheets/sheet1.xml
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val sheetBuilder = StringBuilder()
            sheetBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sheetBuilder.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

            // 列宽
            sheetBuilder.append("<cols>")
            val widths = intArrayOf(22, 35, 10, 10, 16, 20, 20, 20, 22)
            widths.forEachIndexed { i, w ->
                sheetBuilder.append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""")
            }
            sheetBuilder.append("</cols>")

            sheetBuilder.append("<sheetData>")

            // 表头行（s="1" 引用样式索引1 = 加粗+灰底）
            sheetBuilder.append("""<row r="1">""")
            HEADERS.forEachIndexed { i, _ ->
                val col = ('A' + i)
                val idx = stringIndex[HEADERS[i]]!!
                sheetBuilder.append("""<c r="${col}1" t="s" s="1"><v>$idx</v></c>""")
            }
            sheetBuilder.append("</row>")

            // 数据行
            rows.forEachIndexed { rowIdx, row ->
                val rowNum = rowIdx + 2
                sheetBuilder.append("""<row r="$rowNum">""")
                row.forEachIndexed { colIdx, strIdx ->
                    val col = ('A' + colIdx)
                    sheetBuilder.append("""<c r="${col}${rowNum}" t="s"><v>$strIdx</v></c>""")
                }
                sheetBuilder.append("</row>")
            }

            sheetBuilder.append("</sheetData></worksheet>")
            zip.write(sheetBuilder.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    // ── Markdown 导出 ──

    private fun exportMarkdown(
        tasks: List<Task>,
        file: File,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ) {
        val startStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val endStr = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.write("# NextThing 任务导出\n\n")
                writer.write("**时间范围**: $startStr ~ $endStr\n\n")
                writer.write("**任务总数**: ${tasks.size}\n\n")

                val completed = tasks.count { it.status == TaskStatus.COMPLETED }
                val pending = tasks.count { it.status == TaskStatus.PENDING }
                val overdue = tasks.count { it.status == TaskStatus.OVERDUE }
                writer.write("| 统计项 | 数量 |\n|--------|------|\n")
                writer.write("| 已完成 | $completed |\n| 待完成 | $pending |\n| 已逾期 | $overdue |\n\n---\n\n")

                writer.write("| 标题 | 分类 | 状态 | 优先级 | 截止时间 | 创建时间 |\n")
                writer.write("|------|------|------|--------|----------|----------|\n")
                tasks.forEach { task ->
                    writer.write("| ${escapeMd(task.title)} | ${task.category.displayName} | ${statusToText(task.status)} | ${task.importanceUrgency?.displayName ?: "-"} | ${task.dueDate?.format(DATE_FORMAT) ?: "-"} | ${task.createdAt.format(DATE_FORMAT)} |\n")
                }

                val tasksWithDesc = tasks.filter { it.description.isNotBlank() }
                if (tasksWithDesc.isNotEmpty()) {
                    writer.write("\n---\n\n## 任务详情\n\n")
                    tasksWithDesc.forEach { task ->
                        writer.write("### ${task.title}\n\n")
                        writer.write("- **状态**: ${statusToText(task.status)}\n")
                        writer.write("- **分类**: ${task.category.displayName}\n")
                        if (task.tags.isNotEmpty()) {
                            writer.write("- **标签**: ${task.tags.joinToString(", ")}\n")
                        }
                        writer.write("\n${task.description}\n\n")
                    }
                }
            }
        }
    }

    private fun escapeMd(value: String): String {
        return value.replace("|", "\\|").replace("\n", " ")
    }

    // ── 辅助方法 ──

    private fun taskToRow(task: Task): List<String> {
        return listOf(
            task.title,
            task.description.replace("\n", " "),
            task.category.displayName,
            statusToText(task.status),
            task.importanceUrgency?.displayName ?: "",
            task.dueDate?.format(DATE_FORMAT) ?: "",
            task.createdAt.format(DATE_FORMAT),
            task.completedAt?.format(DATE_FORMAT) ?: "",
            task.tags.joinToString(", ")
        )
    }

    private fun statusToText(status: TaskStatus): String {
        return when (status) {
            TaskStatus.PENDING -> "待完成"
            TaskStatus.COMPLETED -> "已完成"
            TaskStatus.DELAYED -> "已延期"
            TaskStatus.OVERDUE -> "已逾期"
            TaskStatus.CANCELLED -> "已放弃"
        }
    }
}
