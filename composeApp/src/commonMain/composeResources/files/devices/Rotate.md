# Rotate

The Rotate device alters the position of incoming LED signals, effectively rotating their coordinates by 90, 180, or 270 degrees. It's most commonly used to easily change the orientation of light patterns and animations without having to manually redraw them.

![Rotate device](res://rotate.jpg)

*Rotate device*

The UI provides a Mode dropdown to select the degree of rotation. Below the dropdown, there are two checkboxes: Isolate and Bypass. 

When a signal enters the device, its bounds are calculated. The device then maps the incoming X and Y coordinates to new coordinates based on the selected rotation angle within those bounds. The adjusted signals are then passed along to the next device in the chain.

The way the device operates depends on the selected mode:
* **90°** – Rotates the incoming signals by 90 degrees clockwise.
* **180°** – Rotates the incoming signals by 180 degrees (upside down).
* **270°** – Rotates the incoming signals by 270 degrees clockwise (or 90 degrees counter-clockwise).

If the Isolate checkbox is checked, the rotation occurs relative to the boundaries of the specific launchpad device that originated the signal, rather than the entire workspace boundaries.
If the Bypass checkbox is checked, the original un-rotated signals are kept and passed along with the newly rotated signals.
