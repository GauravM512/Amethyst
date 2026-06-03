import java.util.regex.Pattern

fun main() {
    val text = "![Keyframes device](res://keyframes.png)"
    val regex = Regex("""!\[.*?\]\((.*?)\)""")
    val match = regex.find(text)
    println(match?.groupValues?.get(1))
}
