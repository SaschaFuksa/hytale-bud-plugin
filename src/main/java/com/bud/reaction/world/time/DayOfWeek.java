package com.bud.reaction.world.time;

public enum DayOfWeek {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;

    public static DayOfWeek fromDayCount(long dayCount) {
        DayOfWeek[] values = values();
        return values[(int) (dayCount % values.length)];
    }
}
