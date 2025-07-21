package pro.finstream.sso.support.auth;

import java.util.Collection;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import pro.finstream.sso.domain.login.UserInfo;
import pro.finstream.sso.support.error.BaseException;
import pro.finstream.sso.support.error.ErrorCode;

public class AuthenticationToken implements Authentication {
    
    private final UserInfo userInfo;

    public AuthenticationToken(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userInfo.roles();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userInfo;
    }

    @Override
    public boolean isAuthenticated() {
        return userInfo.authenticated();
    }

    @Override
    public String getName() {
        return userInfo.username();
    }

    @Override
    public boolean equals(Object another) {
        return another instanceof AuthenticationToken other && Objects.equals(userInfo.id(), other.userInfo.id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(userInfo.id());
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        throw new BaseException(ErrorCode.E_NOT_SUPPORTED);
    }
}
