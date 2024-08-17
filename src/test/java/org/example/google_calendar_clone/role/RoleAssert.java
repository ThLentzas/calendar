package org.example.google_calendar_clone.role;

import org.assertj.core.api.AbstractAssert;

import org.example.google_calendar_clone.entity.Role;

class RoleAssert extends AbstractAssert<RoleAssert, Role> {

    RoleAssert(Role actual) {
        super(actual, RoleAssert.class);
    }

    static RoleAssert assertThat(Role actual) {
        return new RoleAssert(actual);
    }

    /*
        Bellow method checks if the id is generated, but i think we don't have to assert that because we can trust
        Hibernate that it will generate the id of an entity upon saving.

        RoleAssert hasId() {
            isNotNull();
            if(actual.getId() == null) {
                failWithMessage("Expected role ID to be non-null but was null");
            }

            return this; // Return this to allow chaining of assertions
        }
     */
    RoleAssert hasType(RoleType expectedType) {
        isNotNull();
        if (!actual.getType().equals(expectedType)) {
            failWithMessage("Expected role type to be <%s> but was <%s>", expectedType, actual.getType());
        }

        return this; // Return this to allow chaining of assertions
    }
}
