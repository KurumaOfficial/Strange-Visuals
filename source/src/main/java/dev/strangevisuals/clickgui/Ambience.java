package dev.strangevisuals.clickgui;

public final class Ambience {
    private Ambience() {
    }

    public static long timeOfDay(long vanilla) {
        ModuleInfo module = ModuleRegistry.ambience();
        if (module == null || !module.enabled()) {
            return vanilla;
        }

        long target = switch (ModuleRegistry.ambienceTime().current()) {
            case "Dawn" -> 23200L;
            case "Morning" -> 1500L;
            case "Noon" -> 6000L;
            case "Sunset" -> 12200L;
            case "Dusk" -> 13400L;
            case "Midnight" -> 18000L;
            default -> (long) ModuleRegistry.ambienceCustom().value();
        };

        long day = Math.floorDiv(vanilla, 24000L);
        return day * 24000L + Math.floorMod(target, 24000L);
    }
}
