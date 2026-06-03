# Hold

The Hold device sustains an incoming signal for a predetermined duration before releasing it. It's most commonly used to extend short trigger notes into longer sustained notes or to create rhythmic, gated patterns.

![Hold device](res://hold.jpg)

*Hold device*

The device interface features two main dials to shape the hold length:
* **Hold dial**: Sets the base duration the signal will be held. This can be synchronized to a musical division (like 1/4 or 1/8 notes) or set to a specific time in milliseconds.
* **Gate dial**: Scales the final hold time as a percentage of the Hold dial's value. Setting this to 100% applies the exact duration, 50% cuts the time in half, and 200% doubles the duration.

The way the device operates depends on the selected mode:
* **Trigger** – The hold effect starts exactly when the signal arrives and releases after the calculated duration, regardless of how long the incoming signal itself is held.
* **Minimum** – The signal will be sustained for at least the specified duration. If the original incoming signal is held longer than this duration, it won't be released until the original signal is physically released.
* **Infinite** – The signal is sustained indefinitely and never automatically released.

If the **On Release** checkbox is checked, the device will wait until the incoming signal ends (is released) before triggering a new held signal for the specified duration.
