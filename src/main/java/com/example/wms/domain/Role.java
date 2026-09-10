package com.example.wms.domain;

/**
 * Three roles now:
 *  - PENDING: default for every new self-registered account and every new
 *    OAuth2 sign-in. Grants no access at all — an ADMIN has to promote the
 *    account before it can do anything (see UserAdminService).
 *  - VISITOR: normal day-to-day access (receive/pick/view).
 *  - ADMIN: full access, including warehouse/product setup and assigning
 *    roles to other users.
 *
 * Still a plain enum rather than a Role/Permission table — three fixed,
 * non-overlapping roles is still well short of where that table earns
 * its complexity.
 */
public enum Role {
    PENDING,
    VISITOR,
    ADMIN
}
