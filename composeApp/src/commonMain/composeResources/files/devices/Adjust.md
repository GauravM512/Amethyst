# Adjust

The Adjust device modifies the incoming LED signal's colors by applying adjustments to brightness, contrast, temperature, and tint. It's most commonly used to fine-tune and correct color properties within a chain.

![Adjust device](res://adjust.jpg)

*Adjust device*

The Adjust device offers four text dials for precise color adjustments:
* **Brightness** – Multiplicatively scales the brightness of the incoming colors. The dial ranges from 0% to 200%, with 100% being no change.
* **Contrast** – Adjusts the color contrast relative to a middle-gray point. The dial ranges from 0% to 200%.
* **Temp** – Adjusts the color temperature from -100 to 100. Positive values add warmth by raising red and lowering blue, while negative values add coolness by raising blue and lowering red.
* **Tint** – Shifts the color balance between green and magenta from -100 to 100. Positive values increase green, while negative values increase magenta (by boosting red and blue).

When a signal enters the device, it processes each visible LED color sequentially: it applies the brightness multiplier, applies the contrast adjustment around a pivot of 0.5, adds the temperature offset, and applies the tint shift. Transparent colors are ignored and pass through the device unaffected.
