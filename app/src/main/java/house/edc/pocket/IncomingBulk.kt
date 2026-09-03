package house.edc.pocket

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun buildIncomingZip(files: List<Pair<String, ByteArray>>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        val used = HashSet<String>()
        files.forEach { (name, bytes) ->
            var entryName = name.substringAfterLast('/').ifBlank { "edc-file" }
            var suffix = 1
            while (!used.add(entryName)) {
                val dot = entryName.lastIndexOf('.')
                entryName = if (dot > 0) {
                    "${entryName.substring(0, dot)}-$suffix${entryName.substring(dot)}"
                } else {
                    "$entryName-$suffix"
                }
                suffix++
            }
            zip.putNextEntry(ZipEntry(entryName))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return out.toByteArray()
}
