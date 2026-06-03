# Group

The Group device splits the incoming signal into multiple parallel processing chains, known as groups. It's most commonly used for layering different effects, creating complex parallel processing paths, or organizing intricate device combinations.

![Group device](res://group.jpg)

*Group device*

The interface consists of a list of groups and a main editor area for the currently selected group's internal device chain. You can create, rename, rearrange, duplicate, copy, paste, and delete groups using the list controls. Selecting a group opens its internal chain, where you can drag and drop other devices to build a unique processing path for that specific group. Additionally, there is an automapping button that allows you to map parameters from devices inside the groups to external macro controls.

When the Group device receives an incoming signal, it passes the signal simultaneously into every group's internal chain. Each group processes its own independent copy of the signal in parallel, and the outputs of all groups are typically mixed together at the end.
