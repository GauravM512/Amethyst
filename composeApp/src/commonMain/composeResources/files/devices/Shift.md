# Shift

The Shift device adjusts the Hue, Saturation, and Value (brightness) of incoming LED signals. It's most commonly used for color correction, tinting signals, or constraining colors to a specific saturation and brightness range.

![Shift device](res://shift.jpg)

*Shift device*

The Shift device provides controls to independently modify the HSV components of the signal:
* **Hue**: A dial that shifts the color hue by a specified degree (-180° to 180°). This rotates the color around the color wheel.
* **Sat Low** & **Sat High**: Dials that map the incoming saturation to a new range. This allows you to set a minimum or maximum saturation, or compress the saturation range.
* **Val Low** & **Val High**: Dials that map the incoming value (brightness) to a new range. This allows you to ensure colors don't get too dark or too bright, or constrain the brightness entirely.

When an LED signal enters the device, it first converts the color to HSV (Hue, Saturation, Value). It then adds the Hue shift, scales the Saturation between the Sat Low and Sat High bounds, and scales the Value between the Val Low and Val High bounds. Finally, it converts the modified HSV values back into RGB to output the shifted signal.
