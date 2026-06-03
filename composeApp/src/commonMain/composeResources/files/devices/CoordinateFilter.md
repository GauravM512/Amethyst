# Coordinate Filter

The Coordinate Filter device filters incoming signals, only allowing those with specific coordinates to pass through. It's most commonly used to isolate certain pads on your Launchpad, such as mapping specific buttons to certain effects or preventing certain pads from triggering other devices.

![Coordinate Filter device](res://coordinate_single.jpg)

*Coordinate Filter device*

When you only have one Launchpad in your workspace, the device displays a miniature virtual representation of it. You can interact with this virtual Launchpad by clicking and dragging over its pads to select which coordinates to filter. Selected pads will light up green to indicate they are active. If you have multiple Launchpads, the device displays a "Pick" button instead. Clicking this button enters a special selection mode where you can click and drag directly on the main workspace Launchpads to add them to the filter.

The device intercepts incoming MIDI and LED signals. It checks each signal's (X, Y) coordinates against the list of selected pads. If an incoming signal's coordinates match any of the selected pads, it is allowed to pass through to the next device in the chain. Any signals that do not match the selected coordinates are blocked and ignored.
