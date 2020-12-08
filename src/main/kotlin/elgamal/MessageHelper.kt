package elgamal

import java.io.File

object MessageHelper {
    private fun getNewMessage(): String {
        return "Какое-то простое сообщение для схемы подписи Эль-Гамаля на основе эллиптических кривых"
    }

    fun saveMessageToPath(path: String) {
        File(path).apply {
            if (!exists()) {
                createNewFile()
            }
            writeText(getNewMessage())
        }
    }

    fun readMessageFromFile(path: String): String? {
        try {
            return File(path).readText()
        } catch (e: Exception) {
            println("Ошибка чтения сообщения из файла. Сообщение будет сгенерировано заново")
            return null
        }
    }
}