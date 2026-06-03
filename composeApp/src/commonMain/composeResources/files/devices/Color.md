# Color

The Color device replaces the color of all active incoming LED signals with a single, uniform color. It's most commonly used to override animated patterns with a specific static color, or to forcefully colorize an LED sequence.

![Color device](res://color.jpg)

*Color device*

The UI consists of a large square color picker where you can click and drag to select the saturation and lightness of your desired color. To the right of the square, a vertical hue picker bar lets you choose the base hue. Below the color picker, a hex color text field allows you to manually input or copy a specific hex color code.

The device processes incoming signals by analyzing the color of each LED. If an LED's color is not completely black (meaning it is actively lit), the device replaces its color with the color currently selected in the UI. Black LEDs are passed through unchanged, which preserves the original shape and spacing of animations without lighting up inactive pixels.
