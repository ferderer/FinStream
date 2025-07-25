package pro.finstream.sso.domain.login;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    SYSTEM, ADMIN, CREATOR, USER;

    @Override
    public String getAuthority() {
        return name();
    }
}
