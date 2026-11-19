package com.jewelerp.model;

import java.util.List;

public final class Role {

    public static final String ADMIN = "admin";
    public static final String MANAGER = "manager";
    public static final String CASHIER = "cashier";
    public static final String SALESPERSON = "salesperson";
    public static final String ARTISAN = "artisan";

    public static final List<String> ALL = List.of(ADMIN, MANAGER, CASHIER, SALESPERSON, ARTISAN);

    private Role() {
    }

    public static boolean isManagerial(String role) {
        return ADMIN.equals(role) || MANAGER.equals(role);
    }
}
