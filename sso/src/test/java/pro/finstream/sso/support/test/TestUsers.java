package pro.finstream.sso.support.test;

import java.util.Set;
import pro.finstream.sso.domain.login.Role;

public interface TestUsers {
    record User(long id, String username, String password, Set<Role> roles) {}

    User VADIM_TEST = new User(8620324627478865L, "vadim-test", "pan82k58n", Set.of(Role.USER));
}
