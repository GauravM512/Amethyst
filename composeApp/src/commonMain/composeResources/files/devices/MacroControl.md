# Macro Control

The Macro Control device updates a workspace macro to a specific value whenever it receives an active incoming signal. It's most commonly used to automatically change macro states triggered by specific MIDI inputs or preceding devices in a chain.

![Macro Control device](res://macro_control.jpg)

*Macro Control device*

The device features two main controls:
* **Macro Dial**: Allows the user to select which macro to control (if multiple macros are available). If only one macro exists, it simply displays "Macro 1". If no macros exist, it will display a "No macros available" message.
* **Value Dial**: A dial that sets the target value (ranging from 0 to 127) to apply to the chosen macro.

When an active incoming signal is received (a non-black LED signal or a MIDI signal with a velocity greater than 0), the device passes the signal through to the next device and sets the chosen macro to the target value. Inactive signals (like a black LED color or a MIDI velocity of 0) are simply passed through without triggering a macro change.
