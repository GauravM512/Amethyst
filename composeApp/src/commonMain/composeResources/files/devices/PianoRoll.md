# Piano Roll

The Piano Roll device stores a sequence of LED signals and plays them back when triggered by an incoming signal. It's most commonly used to play complex multi-frame light animations or custom LED sequences from a simple input trigger.

![Piano Roll device](res://piano_roll.jpg)

*Piano Roll device*

The device interface features a single button with a music note icon. Clicking this button opens the full-screen Piano Roll editor, where you can arrange notes, define their timings and durations, and customize their LED colors or gradients.

When the device receives a non-black LED signal or a MIDI signal with a velocity greater than zero, it triggers the playback of the stored sequence. The device automatically maps the pitches of the sequenced notes to X and Y coordinates on the launchpad grid and emits the corresponding LED colors according to their scheduled timings.


Press a Launchpad pad to preview the current solid color or gradient on the device and in the editor. Gradients repeat for as long as the pad is held, using the selected note's duration or the current grid length. Release the pad to end its preview. Turn Preview off in the toolbar to disable this feedback.

Space starts playback at the cursor set in the ruler or an empty grid cell. The playhead crosses the ruler and pad lanes, and active notes are highlighted. Follow keeps the playhead visible as playback advances. Fold shows only pad lanes used by the clip; turn it off to add notes on other pads.

Use B to toggle Draw, double-click to create or delete notes, and Cmd/Ctrl + D to duplicate a selection. Arrow keys move selected notes; Shift + Left/Right adjusts their lengths. Cmd/Ctrl + U quantizes note starts to the current grid. Changes support Undo and Redo. Zoom and Fit controls are available in the toolbar.
