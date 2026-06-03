# Loop

The Loop device repeats incoming signals to create rhythmic echoes or continuous looping sequences. It's most commonly used for creating stutter effects, arpeggiator-like rhythmic patterns, or sustaining a continuous stream of signals while a pad is held.

![Loop device](res://loop.jpg)

*Loop device*

The device's interface provides the following controls:
* **Repeat dial:** Sets the exact number of times the incoming signal will repeat (from 1 to 128). This dial is disabled and displays "Unused" when the Hold checkbox is enabled.
* **Hold checkbox:** Toggles the behavior between playing a fixed number of repetitions and looping continuously while the input is active.
* **Delay dial:** Defines the base time interval between each repetition. This timing can be synchronized to the project tempo (e.g., 1/4 note, 1/8 note) or set to an absolute time in milliseconds.
* **Gate dial:** Modifies the spacing between repeats relative to the base Delay timing, scaling from 0% to 200%. A value of 100% spaces the repeats exactly at the base Delay timing. Values below 100% compress the timing, making repeats occur closer together, while values above 100% stretch the timing. Right-clicking this dial quickly resets it to the default 100%.

When a valid "on" signal (such as an LED color or a MIDI note with velocity) enters the device, it generates multiple identical output signals spaced apart in time according to the Delay and Gate settings. 

The way the device operates depends on the selected mode:
* **Fixed Repeat (Hold unchecked):** The device instantly schedules the signal to repeat exactly the number of times set by the Repeat dial. Releasing the input key has no effect; the full sequence of repeats will always complete regardless of when the key is released.
* **Hold Loop (Hold checked):** The device continuously repeats the signal as long as the input is held down. When the input key is released, the repetition stops immediately, and a final "off" signal (such as a black LED color or zero MIDI velocity) is sent to clean up the loop.
