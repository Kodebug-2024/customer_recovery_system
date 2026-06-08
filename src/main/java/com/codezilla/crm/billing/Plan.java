package com.codezilla.crm.billing;

public enum Plan {
    FREE,
    PRO;

    public static Plan parse(String s) {
        if (s == null) return FREE;
        try { return Plan.valueOf(s.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { return FREE; }
    }
}
