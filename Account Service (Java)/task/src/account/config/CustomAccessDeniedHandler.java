package account.config;

import account.entity.enums.ActionType;
import account.service.EventLogServicePostEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final EventLogServicePostEvent eventLogService;

    @Autowired
    public CustomAccessDeniedHandler(EventLogServicePostEvent eventLogService) {
        this.eventLogService = eventLogService;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        response.sendError(HttpStatus.FORBIDDEN.value(), "Access Denied!");
        // EVENT_LOG: ACCESS_DENIED
        eventLogService.postEvent(ActionType.ACCESS_DENIED, request.getRemoteUser(), request.getServletPath(), request.getServletPath());
    }
}
