package dev.anthonyhfm.amethyst.devices

/**
 * Interface for devices that can be choked (stopped/cleared) by a choke device.
 * When a device is choked, it should:
 * - Cancel all scheduled Heaven tasks
 * - Clear any rendered LED signals
 */
interface Chokeable {
    /**
     * Called when this device should be choked.
     * Implementations should cancel all scheduled tasks and clear any active signals.
     */
    fun onChoke()
}
