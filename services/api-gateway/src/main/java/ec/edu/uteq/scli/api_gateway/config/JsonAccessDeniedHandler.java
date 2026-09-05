package ec.edu.uteq.scli.api_gateway.config;

import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.*;
import java.util.*;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper mapper;
    public JsonAccessDeniedHandler(ObjectMapper mapper){this.mapper=mapper;}
    @Override public void handle(HttpServletRequest request,HttpServletResponse response,AccessDeniedException exception) throws IOException {
        response.setStatus(403);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");
        Map<String,Object> error=new LinkedHashMap<>();error.put("timestamp",OffsetDateTime.now(ZoneOffset.UTC));
        error.put("status",403);error.put("code","FORBIDDEN");error.put("error",HttpStatus.FORBIDDEN.getReasonPhrase());
        error.put("message","No tiene permisos para acceder al recurso");error.put("path",request.getRequestURI());
        mapper.writeValue(response.getOutputStream(),error);
    }
}
