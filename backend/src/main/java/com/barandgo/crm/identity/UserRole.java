package com.barandgo.crm.identity;

public enum UserRole {
    CUSTOMER,
    STAFF,
    MANAGER,
    ADMIN;

    /** Personal del local: encargado, trabajador o administrador. */
    public boolean isBackOffice() {
        return this != CUSTOMER;
    }
}
