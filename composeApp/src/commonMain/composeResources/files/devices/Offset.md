# Offset

The Offset device shifts the position of incoming LED signals along the X and Y axes. It's most commonly used to move animations, patterns, or static shapes to different areas of the Launchpad grid.

![Offset device](res://offset.jpg)

*Offset device*

The UI displays the current X and Y offset values at the top. Below the coordinates, an array of eight directional buttons (arranged like a compass) allows you to move the signals. Clicking any of the arrows shifts the offset by one unit in that specific direction.

Incoming signals are modified by adding the configured X offset to their X coordinate and adjusting their Y coordinate accordingly. After applying the offset, the device will process the signals.
