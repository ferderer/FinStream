package pro.finstream.sso.domain.login;

import java.util.Set;

public record UserInfo(
    Long id,
    String email,
    String username,
    Set<Role> roles,
    boolean authenticated
) {}
