package com.brightloop.subscription.common.enums;

/**
 * Application roles used for RBAC.
 *
 * Stored as a String in the DB (EnumType.STRING) so that the column
 * value matches the enum constant name exactly (e.g. "ADMIN").
 * This is safer than EnumType.ORDINAL — adding/reordering enum
 * constants won't corrupt existing rows.
 *
 * Spring Security authority convention: roles are prefixed with "ROLE_"
 * when used in hasRole() / @PreAuthorize("hasRole('ADMIN')").
 * We store WITHOUT the prefix; the prefix is added in CustomUserDetails.
 */
public enum Role {
    /** Full system access — can manage users, staff, and all data. */
    ADMIN,

    /** Internal staff — can manage customers and view data but not manage other staff. */
    INTERNAL,

    /** End-customer — can view/update their own profile and subscriptions. */
    CUSTOMER
}
