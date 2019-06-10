package mvcp.adobe.controllers;

import mvcp.adobe.entities.CacheManager;
import mvcp.adobe.entities.Request;
import mvcp.adobe.exceptions.CacheNotAvailableException;
import mvcp.adobe.exceptions.NoAvailableEndpointsException;
import mvcp.adobe.proxy.ReverseProxy;
import mvcp.adobe.entities.Response;
import mvcp.adobe.exceptions.ServiceHostNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Unique controller of the application. It's responsible for intercepting all HTTP requests for routing.
 */
@RequestMapping("/")
@RestController
public class Entrypoint {
    public static final Logger logger = (Logger) LoggerFactory.getLogger(Entrypoint.class);

    @Autowired
    private Environment env;

    @Autowired
    private ReverseProxy proxy;

    @Autowired
    private CacheManager cache;


    /**
     * Captures all incoming HTTP requests.
     * The requests are processed into internal structure, then forwarded to the target Service where it will be
     * load-balaced. Finally, receives and returns back the response to the caller.
     *
     * This endpoint captures all HTTP method requests.
     *
     * @param body Map  Payload of the request
     * @param r HTTPServletRequest Object containing information about the HTTP request
     * @return ResponseEntity HTTP response
     */
    @RequestMapping(value = "**")
    public ResponseEntity<String> get(@RequestBody(required = false) Map<String, String> body, HttpServletRequest r) {
        try {
            Request req = Request.fromContextRequest(r, body);
            Response response = null;
            try {
                response = cache.getCached(req);
                if (response != null) {
                    logger.info("+++++CACHE HIT+++++");
                } else {
                    logger.info("+++++CACHE MISS+++++");
                    response = proxy.processRequest(req);
                    cache.store(req, response);
                }
            } catch (CacheNotAvailableException cnae) {
                logger.info("+++++Cache server not available. Executing request+++++");
                logger.info("+++++TRACE: (" + cnae.getMessage() + ")+++++");
                response = proxy.processRequest(req);
            }

            HttpHeaders headers = new HttpHeaders();
            if (response.getHeaders() != null) {
                for (String key : response.getHeaders().keySet()) {
                    String value = response.getHeaders().get(key);
                    if (key == null) continue;
                    headers.set(key, value);
                }
            }

            return new ResponseEntity<>(response.getBody(), headers, HttpStatus.resolve(response.getStatus()));
        } catch (Exception e) {
            e.printStackTrace();
        } catch (ServiceHostNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), null, HttpStatus.NOT_FOUND);
        } catch (NoAvailableEndpointsException e) {
            return new ResponseEntity<>(e.getMessage(), null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return null;
    }
}