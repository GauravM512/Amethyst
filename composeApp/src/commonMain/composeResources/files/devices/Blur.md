# Blur

The Blur device applies a spatial blur effect to the incoming LED signal, spreading light and colors to adjacent pads. It's most commonly used to create glowing auras around lit pads, smooth out sharp shapes, or simulate light dispersion across the grid.

![Blur device](res://blur.jpg)

*Blur device*

The Blur device UI provides several controls to fine-tune the effect:
* **Radius dial:** Sets the maximum distance the blur will spread from the source pad (from 1 to 8).
* **Amount dial:** Controls the blend between the original signal and the blurred signal (from 0% to 100%).
* **Shape dropdown:** Selects the geometric pattern used to calculate the blur area.
* **Curve dropdown:** Changes how the blur intensity attenuates over distance.
* **Edge dropdown:** Determines how the blur behaves when it reaches the boundaries of the 10x10 grid.

When an incoming LED signal enters the device, it calculates the surrounding neighborhood for each active pad based on the radius and shape. It spreads the full color to neighboring pads while the opacity fades over distance based on the chosen attenuation curve. It then blends the dry and wet opacity based on the amount parameter, outputting the resulting glowing pads.

The way the device operates depends on the selected Shape mode:
* **Circle** – The blur spreads outward in a circular pattern based on true distance.
* **Square** – The blur spreads outward to form a square boundary.
* **Diamond** – The blur spreads outward in a diamond shape.

The way the device operates depends on the selected Curve mode:
* **Linear** – The blur's intensity fades evenly over the distance.
* **Ease In** – The blur stays intense near the center and drops off more sharply toward the edge.
* **Ease Out** – The blur intensity drops off quickly near the center and then fades slowly outward.
* **Bell** – The blur uses a Gaussian-style bell curve for a soft, natural fade.

The way the device operates depends on the selected Edge mode:
* **None** – The blur continues out of bounds normally, ignoring grid constraints.
* **Clamp** – The blur stops expanding if it attempts to exceed the limits of a 10x10 grid.
* **Wrap** – The blur wraps around the 10x10 grid; light passing off the right edge reappears on the left, and top edge off the bottom.
