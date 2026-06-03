# Macro Filter

The Macro Filter device selectively blocks or passes incoming signals based on the current value of a specific macro. It's most commonly used for creating conditional logic in a chain, allowing signals to flow only when a macro is set to predefined values.

![Macro Filter device](res://macro_filter.jpg)

*Macro Filter device*

The Macro Filter's interface consists of a **Macro dial** and a 10x10 **Value Grid**:
* **Macro dial**: Allows you to choose which macro (by its number) the device should monitor.
* **Value Grid**: Represents macro values from 0 to 99. You can click or drag across the cells to select or deselect values for filtering. 
  * Values colored in the primary color are "allowed" values.
  * Values in a muted color are "blocked" values.
  * The cell corresponding to the currently selected macro's active value is highlighted with a thicker border. 
  * You can hover over the grid to see the macro value number for each cell.

When a signal enters the device, it checks the current value of the selected macro. If this value corresponds to an "allowed" (primary colored) cell on the grid, the incoming signal passes through the device successfully. If the value corresponds to a "blocked" (muted) cell, the signal is stopped. If there are no macros available in the workspace, all signals will pass through by default.
