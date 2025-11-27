package dev.anthonyhfm.amethyst.conversion.ableton.utils

class MaxParam(
    private val parameterList: XmlElement
) {
    fun getEnumValue(id: Int): Int {
        return parameterList.querySelector("MxDEnumParameter").find {
            it.attributes["Id"] == "$id"
        }?.querySelector("Manual")
            ?.first()
            ?.attributes["Value"]
            ?.toIntOrNull() ?: error("Enum parameter with id $id not found")
    }

    fun getIntValue(id: Int): Int {
        return parameterList.querySelector("MxDIntParameter").find {
            it.attributes["Id"] == "$id"
        }?.querySelector("Manual")
            ?.first()
            ?.attributes["Value"]
            ?.toIntOrNull() ?: error("Int parameter with id $id not found")
    }

    fun getFloatValue(id: Int): Float {
        return parameterList.querySelector("MxDFloatParameter").find {
            it.attributes["Id"] == "$id"
        }?.querySelector("Manual")
            ?.first()
            ?.attributes["Value"]
            ?.toFloatOrNull() ?: error("Float parameter with id $id not found")
    }
}