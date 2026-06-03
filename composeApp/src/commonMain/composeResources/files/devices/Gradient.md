# Gradient

The Gradient device replaces the color of an incoming signal with a custom animated color sequence over time. It's most commonly used to create complex, multi-color animations and fading effects that trigger when a signal passes through.

![Gradient device](res://gradient.jpg)

*Gradient device*

The UI contains a main Gradient Editor bar where you can click to add new color points, drag them to adjust their timing, and right-click them to choose different interpolation curves (smoothness). When a color point is selected, a Color Picker and Hex Editor appear to the right, allowing you to change its exact color. Below the editor, there is a **Duration** dial to control the base length of the full sequence. The **Gate** dial acts as a multiplier for the duration (from 0% to 200%). The **Steps** dial lets you quantize the gradient into a specific number of discrete color changes (from 2 to 16, or INF for continuous smooth fading). 

When a lit signal enters the device, it generates the full sequence of colors and begins animating them over the specified duration and gate length. It overrides the incoming color with the calculated gradient colors. If the incoming signal goes completely black and Loop is enabled, the animation is immediately stopped.

The way the device interpolates between color points depends on the selected curve (smoothness) applied to each point:
* **Linear** – Fades evenly to the next color at a constant rate.
* **Smooth** – Eases in and out for a softer transition to the next color.
* **Sharp** – A more dramatic transition that stays closer to the endpoints before quickly crossing over in the middle.
* **Fast** – Quickly accelerates towards the next color.
* **Slow** – Slowly accelerates towards the next color.
* **Hold** – Stays on the current color and instantly switches to the next color at the very end.
* **Release** – Instantly jumps to the next color at the very beginning.

If the Loop checkbox is checked, the gradient animation will continuously repeat itself as long as the incoming signal is lit.
