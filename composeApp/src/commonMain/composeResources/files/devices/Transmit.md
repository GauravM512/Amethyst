# Transmit

The Transmit device wirelessly routes the incoming signal to other Transmit devices in the workspace. It's most commonly used to transmit signals across multiple chains.

![Transmit device](res://transmit.jpg)

*Transmit device*

The UI features a **Channel** dial and a **Mode** dropdown. The **Channel** dial allows you to select a communication channel from 1 to 16. The **Mode** dropdown sets the role of the device in the network.

The device processes the incoming signal by either broadcasting it to matching receivers on the network or by passing through physically and wirelessly received signals, depending on its configured mode.

The way the device operates depends on the selected mode:
* **Sender** – The device consumes the incoming signal and wirelessly transmits it to all Transmit devices operating as Receivers on the same channel. The signal does not pass through to the Sender's output.
* **Receiver** – The device listens for wirelessly transmitted signals on the selected channel from Senders and passes them to its output. Any physical signal connected directly into the Receiver's input is also passed through to the output.
