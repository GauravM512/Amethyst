# Flip

The Flip device mirrors or flips incoming LED signals across different axes. It's most commonly used for creating symmetrical lighting effects or altering the orientation of a signal.

![Flip device](res://flip.jpg)

*Flip device*

The UI provides a "Mode" dropdown menu to select the axis of reflection for the incoming signals. Below the mode selection, there are two checkboxes: "Isolate" and "Bypass" which alter the scope and behavior of the flipping process.

The device processes the signal by modifying the X and Y coordinates of the incoming LED signals to reflect them according to the selected mode's rules.

The way the device operates depends on the selected mode:
* **Horizontal** – Flips the signal across the horizontal axis, mirroring it from left to right.
* **Vertical** – Flips the signal across the vertical axis, mirroring it from top to bottom.
* **Diagonal+** – Flips the signal diagonally along the top-left to bottom-right axis.
* **Diagonal-** – Flips the signal diagonally along the bottom-left to top-right axis.

If the Isolate checkbox is checked, the device calculates the flip boundaries based on the original device's specific layout (such as a Launchpad's grid) rather than the global workspace boundaries.
If the Bypass checkbox is checked, the original, unflipped signals are passed through the device alongside the newly flipped signals, creating a mirrored duplication.
