# Preview

The Preview device provides a real-time visual representation of the LED signals passing through it, without altering the signals themselves. It's most commonly used for monitoring the output of effects or chains, and allows you to test interactions by clicking its virtual pads to inject signals directly into the chain at its position.

![Preview device](res://preview.jpg)

*Preview device*

The interface dynamically adapts based on the number of connected devices in your project. If you have only a single device connected, the device displays a small, interactive virtual representation of your launchpad directly within the chain. You can click on the pads of this virtual launchpad to trigger signals (simulating pad presses).

If you have multiple devices connected, the device displays a button with an eye icon. Clicking this button opens a dedicated preview mode in the main workspace, allowing you to view and interact with all connected devices simultaneously.

When an incoming LED signal reaches the device, it registers the signal to light up its corresponding virtual pads and then passes the signal unaffected down the rest of the chain.
