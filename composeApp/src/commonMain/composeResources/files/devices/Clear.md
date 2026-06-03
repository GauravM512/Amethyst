# Clear

The Clear device detects when an incoming signal is released and triggers a system-wide clearing of lights, audio, or multis. It's most commonly used to stop ongoing effects and audio playback once a key or pad is released.

![Clear device](res://clear.jpg)

*Clear device*

The device interface features a column of three checkboxes: **Lights**, **Audio**, and **Multi**. These controls allow you to choose exactly which components of the application should be reset upon a release.

When signals enter the device, they are immediately passed along to the next device in the chain. The device also inspects each signal to determine if it represents a "release" action. A release is recognized when a MIDI signal has a velocity of 0, or when an LED signal's color is black. Upon detecting a release, the device schedules a brief internal delay before executing the enabled clearing actions.

If the **Lights** checkbox is checked, all active lights in the system are cleared.
If the **Audio** checkbox is checked, all currently playing audio is stopped.
If the **Multi** checkbox is checked, the workspace multi states are reset.
