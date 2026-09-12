package network.rs485.logisticspipes.gui
data class Margin(val top: Int = 0, val left: Int = 0, val bottom: Int = 0, val right: Int = 0) {
    companion object {
        val NONE = Margin()
        val DEFAULT = Margin(6)
    }

    constructor(margin: Int) : this(
        top = margin,
        bottom = margin,
        left = margin,
        right = margin
    )

    val horizontal = left + right
    val vertical = top + bottom
}

enum class HorizontalAlignment {
    LEFT,
    CENTER,
    RIGHT;
}

enum class VerticalAlignment {
    TOP,
    CENTER,
    BOTTOM;
}

enum class Size {
    FIXED,
    GROW,
    MIN
}
