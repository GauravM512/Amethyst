# Multi

The Multi device routes each incoming signal to one of its nested groups in a sequence. It's most commonly used to distribute repeated inputs across multiple different effect chains to create varied, evolving patterns.

![Multi device](res://multi.jpg)

*Multi device*

The device interface features a group editor list, where you can add, remove, rearrange, and select different groups. When a group is selected, its contained chain of devices is displayed for editing. Before signals are routed to a group, they can pass through the preprocess chain on the left, allowing you to insert effects that apply globally to all incoming signals. You can toggle the automapping feature using the target icon in the bottom rail. At the bottom of the group list, a dropdown allows you to select the routing mode.

When a signal enters the device, the device selects a target group according to the current mode. The signal is first passed through the preprocess chain, and the output is then sent to the chosen group's inner chain. When an active signal ends (such as a key being released or a light turning off), the device remembers which group processed the start of the signal and routes the ending signal to the exact same group to ensure consistency.

The way the device operates depends on the selected mode:
* **Forwards** – Each new signal advances to the next group in the list sequentially, returning to the start after reaching the end.
* **Backwards** – Each new signal moves to the previous group in the list sequentially, wrapping around to the end after reaching the start.
* **Random** – Each new signal is assigned to a randomly selected group.
