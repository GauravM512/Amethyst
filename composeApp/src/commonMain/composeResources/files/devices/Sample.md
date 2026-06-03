# Sample

The Sample device plays back a loaded audio file when triggered by an incoming MIDI note. It's most commonly used for triggering one-shot sounds, drum samples, or loops.

![Sample device](placeholder)

*Sample device*

To use the device, first load an audio file using the **Open Sample** button. Once a sample is loaded, a waveform view appears. You can adjust the playback range by dragging the start and end handles on the left and right sides of the waveform. The device also supports volume automation—you can click on the waveform to add envelope points and draw custom volume curves. Alt-clicking and dragging between points allows you to curve the segment.

Below the waveform, there are three dials:
* **Fade In** – Controls how long it takes for the sample to reach full volume when it starts playing (up to 1000 ms).
* **Fade Out** – Controls how long it takes for the sample to fade out at the end of the playback range (up to 1000 ms).
* **Volume** – Adjusts the overall output volume of the sample, ranging from -24 dB to +24 dB.

When a MIDI signal with a velocity greater than 0 enters the device, it processes the loaded sample audio. It applies the selected start and end points, fades, volume adjustments, and any volume envelope automation. Finally, it outputs the processed audio as an audio signal. Any previously playing instance of the sample on this device is stopped when a new note is received.
