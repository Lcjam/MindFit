package com.mindfit.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    @DisplayName("Role은 GrantedAuthority를 구현한다")
    void role_isGrantedAuthority() {
        assertThat((Object) Role.ROLE_ADMIN).isInstanceOf(GrantedAuthority.class);
        assertThat((Object) Role.ROLE_CLIENT).isInstanceOf(GrantedAuthority.class);
        assertThat((Object) Role.ROLE_COUNSELOR).isInstanceOf(GrantedAuthority.class);
    }

    @Test
    @DisplayName("ROLE_CLIENT의 getAuthority는 enum 이름과 동일하다")
    void getAuthority_client_returnsRoleName() {
        assertThat(Role.ROLE_CLIENT.getAuthority()).isEqualTo("ROLE_CLIENT");
    }

    @Test
    @DisplayName("ROLE_COUNSELOR의 getAuthority는 enum 이름과 동일하다")
    void getAuthority_counselor_returnsRoleName() {
        assertThat(Role.ROLE_COUNSELOR.getAuthority()).isEqualTo("ROLE_COUNSELOR");
    }

    @Test
    @DisplayName("ROLE_ADMIN의 getAuthority는 enum 이름과 동일하다")
    void getAuthority_admin_returnsRoleName() {
        assertThat(Role.ROLE_ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}
