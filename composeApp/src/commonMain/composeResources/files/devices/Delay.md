# Delay

The Delay device delays the incoming signal by a specified amount of time before passing it to the next device. It's most commonly used for creating rhythmic variations, staggering events, or adding a time offset to the signal.

![Delay device](res://delay.jpg)

*Delay device*

The Delay device features two primary dials:
* **Delay**: Sets the base duration of the delay. This can be configured as a musical timing (e.g., 1/4 note) or an absolute millisecond value.
* **Gate**: Adjusts the proportion of the delay time that is actually applied, scaled as a percentage (from 0% up to 200%). A value of 100% applies the exact delay time, while lower or higher values shorten or lengthen it respectively. You can right-click the dial to reset it to exactly 100%.

When a signal enters the device, it is held in a scheduled task for a duration calculated by multiplying the base delay time by the gate multiplier. Once this time elapses, the signal is passed to the next device in the chain. Additionally, because the device supports choking, if a choke is triggered, any signals currently waiting in the delay queue are cancelled immediately and will not be outputted.
