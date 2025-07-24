package pro.finstream.broadcasting.common.auth;

public interface CurrentUserAccessor {
    
    default long currentUserId() {
        return 0;
    }
}
