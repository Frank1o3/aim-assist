package aimmod.assist;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "aim-assist")
public class AimAssistConfig implements ConfigData {

    /* ─── Master toggles ─────────────────────────────────────────────── */
    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Gui.Tooltip
    public boolean showGhostMarker = true;

    @ConfigEntry.Gui.Tooltip
    public boolean leadTargets = true; // predictive aim

    @ConfigEntry.Gui.Tooltip
    public boolean compensateDrop = true; // gravity / drag

    /* ─── Range / power ──────────────────────────────────────────────── */
    @ConfigEntry.BoundedDiscrete(min = 4, max = 96)
    @ConfigEntry.Gui.Tooltip
    public int maxAssistDistance = 40;

    /* ─── Physics constants (vanilla‑ish defaults) ───────────────────── */
    @ConfigEntry.Gui.Tooltip(count = 2)
    public double gravity = 0.05; // blocks·tick‑²

    @ConfigEntry.Gui.Tooltip(count = 2)
    public double drag = 0.99; // horizontal drag

    @ConfigEntry.Gui.Tooltip
    public double arrowSpeedFactor = 3.0; // speed = pull × factor

    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    @ConfigEntry.Gui.Tooltip
    public int iterations = 2; // Newton passes

    @ConfigEntry.BoundedDiscrete(min = 1, max = 45)
    @ConfigEntry.Gui.Tooltip(count = 2)
    public int assistFovDegrees = 8; // ± deg cone

    /* ─── Assist strength & response ─────────────────────────────────── */
    @ConfigEntry.BoundedDiscrete(min = 0, max = 45)
    @ConfigEntry.Gui.Tooltip
    public int assistStrengthPercent = 15; // 0 % … 100 %

    @ConfigEntry.BoundedDiscrete(min = 1, max = 90)
    @ConfigEntry.Gui.Tooltip
    public int clamp = 60; // max ° per tick
}
