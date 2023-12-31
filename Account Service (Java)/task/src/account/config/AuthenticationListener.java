package account.config;

import account.entity.enums.ActionType;
import account.exception.ExceptionHandlerController;
import account.service.EventLogServicePostEvent;
import account.service.UserServiceChangeAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationListener implements ApplicationListener<AbstractAuthenticationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerController.class);

    static final int MAX_FAILED_ATTEMPTS = 4;

    final EventLogServicePostEvent eventLogServicePostEvent;
    final UserServiceChangeAccess userServiceChangeAccess;
    final int maxFailedAttempts;

    @Autowired
    public AuthenticationListener(
            EventLogServicePostEvent eventLogServicePostEvent,
            UserServiceChangeAccess userServiceChangeAccess,
            @Value("${login.max-failed-attempts}") int maxFailedAttempts
    ) {
        this.eventLogServicePostEvent = eventLogServicePostEvent;
        this.userServiceChangeAccess = userServiceChangeAccess;
        this.maxFailedAttempts = maxFailedAttempts;
    }

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {

        Object user = event.getAuthentication().getPrincipal();
        String username;
        if (user instanceof UserDetails) {
            username = ((UserDetails) user).getUsername();
        } else {
            username = user.toString();
        }
        int failedAttempts = userServiceChangeAccess.getFailedLoginAttempts(username);
        if (event instanceof AuthenticationFailureBadCredentialsEvent) {
            eventLogServicePostEvent.postEvent(ActionType.LOGIN_FAILED, username, "", "");
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                // EVENT_LOG: BRUTE_FORCE
                eventLogServicePostEvent.postEvent(ActionType.BRUTE_FORCE, username, "", "");
                try {
                    userServiceChangeAccess.changeAccess(username, "LOCK");
                    // EVENT_LOG: LOCK_USER
                    eventLogServicePostEvent.postEvent(ActionType.LOCK_USER, username, "Lock user " + username, "");
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }
            userServiceChangeAccess.setFailedLoginAttempts(failedAttempts + 1, username);
        } else if (event instanceof AuthenticationSuccessEvent) {
            if (failedAttempts > 0) {
                userServiceChangeAccess.setFailedLoginAttempts(0, username);
            }
        }
    }
}
