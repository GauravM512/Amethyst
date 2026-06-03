# Color Filter

The Color Filter device filters the incoming LED signals by only allowing colors that match a specific Hue, Saturation, and Value (HSV) profile to pass through. It's most commonly used to isolate specific colors from a mixed signal or to create conditional color-based effects.

![Color Filter device](res://color_filter.jpg)

*Color Filter device*

The UI provides dials to configure the target HSV values and their respective tolerances:
* **Hue** and **Tolerance**: A step dial sets the target color hue (from -180° to 180°), while the tolerance dial defines the acceptable range of hues around the target (as a percentage).
* **Saturation** and **Tolerance**: Set the desired color saturation and its acceptable tolerance (both as percentages).
* **Value** and **Tolerance**: Set the desired color brightness (value) and its acceptable tolerance (both as percentages).

When a signal enters the device, it evaluates the color of each LED against the configured Hue, Saturation, and Value thresholds. If the color falls within all three tolerance ranges, the signal is allowed to pass to the next device. If a color falls outside the acceptable bounds, it is filtered out and discarded. Fully transparent colors automatically bypass the filter.
