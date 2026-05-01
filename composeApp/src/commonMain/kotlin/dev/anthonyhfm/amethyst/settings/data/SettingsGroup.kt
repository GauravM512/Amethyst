package dev.anthonyhfm.amethyst.settings.data

abstract class SettingsGroup(val title: String) {

    private val _settings = mutableListOf<Setting<*>>()
    val settings: List<Setting<*>> = _settings

    protected fun toggle(
        key: String,
        title: String,
        default: Boolean,
        onUpdate: (Boolean) -> Unit = {},
    ): Setting.Toggle = Setting.Toggle(key, title, default, onUpdate).also { _settings += it }

    protected fun <T> select(
        key: String,
        title: String,
        default: T,
        options: List<T>,
        codec: SettingCodec<T>,
        label: (T) -> String = { it.toString() },
        onUpdate: (T) -> Unit = {},
    ): Setting.Select<T> = Setting.Select(key, title, default, options, codec, label, onUpdate).also { _settings += it }

    protected fun slider(
        key: String,
        title: String,
        default: Float,
        range: ClosedFloatingPointRange<Float> = 0f..1f,
        onUpdate: (Float) -> Unit = {},
    ): Setting.Slider = Setting.Slider(key, title, default, range, onUpdate).also { _settings += it }

    protected fun text(
        key: String,
        title: String,
        default: String = "",
        onUpdate: (String) -> Unit = {},
    ): Setting.TextField = Setting.TextField(key, title, default, onUpdate).also { _settings += it }
}
