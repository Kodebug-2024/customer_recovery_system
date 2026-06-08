package com.codezilla.crm.user;

import java.util.Set;

public final class Roles {
    public static final String OWNER = "OWNER";
    public static final String ADMIN = "ADMIN";
    public static final String AGENT = "AGENT";
    public static final String VIEWER = "VIEWER";

    public static final Set<String> ALL = Set.of(OWNER, ADMIN, AGENT, VIEWER);

    private Roles() {}

    public static boolean isValid(String role) {
        return role != null && ALL.contains(role);
    }
}
