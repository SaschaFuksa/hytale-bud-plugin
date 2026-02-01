package com.bud;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class AttitudeExplorationTest {

    public void exploreAttitudeClasses() {
        System.out.println("=== ATTITUDE EXPLORATION ===");

        tryFindClass("com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap");

        System.out.println("=== END EXPLORATION ===");
    }

    private void tryFindClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            System.out.println("Found class: " + clazz.getName());
            for (Method m : clazz.getMethods()) {
                String params = Arrays.stream(m.getParameterTypes())
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", "));
                System.out.println("  " + m.getReturnType().getSimpleName() + " " + m.getName() + "(" + params + ")");
            }
        } catch (Throwable e) {
            System.out.println("Not found or Error: " + className + " " + e);
            e.printStackTrace();
        }
    }
}
