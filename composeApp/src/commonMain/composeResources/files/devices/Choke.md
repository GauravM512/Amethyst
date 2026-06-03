# Choke

The Choke device mutes or stops the activity of other devices assigned to the same channel whenever it receives an incoming signal, while simultaneously passing the signal through to its own nested chain. It's most commonly used for creating light effects or samples that are cutting off whenever a new one triggers.

![Choke device](res://choke.jpg)

*Choke device*

The main control is the "Target" step dial, which assigns the device to a specific choke group channel (from 1 to 16). Additionally, the Choke device acts as a container; you can drag and drop other devices into its nested chain to be processed.

When a signal enters the device, it first sends a command to choke (mute) all other Choke devices that share the same Target channel. It then passes the incoming signal down to its own nested chain of devices for normal processing.
