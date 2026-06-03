# Piano Roll

The Piano Roll device stores a sequence of LED signals and plays them back when triggered by an incoming signal. It's most commonly used to play complex multi-frame light animations or custom LED sequences from a simple input trigger.

![Piano Roll device](res://piano_roll.jpg)

*Piano Roll device*

The device interface features a single button with a music note icon. Clicking this button opens the full-screen Piano Roll editor, where you can arrange notes, define their timings and durations, and customize their LED colors or gradients.

When the device receives a non-black LED signal or a MIDI signal with a velocity greater than zero, it triggers the playback of the stored sequence. The device automatically maps the pitches of the sequenced notes to X and Y coordinates on the launchpad grid and emits the corresponding LED colors according to their scheduled timings.
