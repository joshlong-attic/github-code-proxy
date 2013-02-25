package com.joshlong.ghproxy;

import com.joshlong.ghproxy.jsonp.JsonWithPadding;
import com.joshlong.ghproxy.jsonp.JsonpCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple controller that simply forwards the request to the
 * appropriate <A href="http://github.com">GitHub</A> repository.
 *
 * @author Josh Long
 */
@Controller
public class GithubProxyController {

    private RestTemplate restTemplate = new RestTemplate();
    private String callbackNameAttribute = "callback";
    private String urlForCode = "https://raw.github.com/{user}/{repo}/{branch}/code";
    private String urlForGist = "https://gist.github.com/{user}/{gist}/raw/";

    protected String contentForGithubGist(String user, String gist) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("gist", gist);
        vars.put("user", user);
        ResponseEntity<String> gistResponse = restTemplate.getForEntity(urlForGist, String.class, vars);
        return gistResponse.getBody();
    }

    protected String contentForGithubPage(String user, String repos, String branch, String filePath) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("user", user);
        vars.put("repo", repos);
        vars.put("branch", branch);
        ResponseEntity<String> code = restTemplate.getForEntity(
                urlForCode + filePath(filePath), String.class, vars);
        return code.getBody();
    }

    private String filePath(String fp) {
        if (!fp.startsWith("/"))
            fp = "/" + fp;
        return fp;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/gist/{user}/{gist}")
    @ResponseBody
    public JsonWithPadding<  String > gist(@PathVariable("user") String user,
                       @PathVariable("gist") String gist,
                       @RequestParam("callback")  String callback ) {


        return new JsonWithPadding<String>( callback, contentForGithubGist(user, gist));

    }

    /*

        // Supports rendering using the Spring MVC specific type, 'JsonWithPadding'
        <CODE>
        public @ResponseBody  JsonWithPadding&lt;Foo&gt;  buildFoo (){
            // ...
        }
        </CODE>
     */

    /*

       // inspects a method level annotation '@JsonpCallback' for information
       // on the name of the JSONP callback method parameter to be found in the request or, alternatively, just specifies the callback

        <CODE>
          @JsonCallback( paddingRequestParameterName = '_cb')
          public  @ResponseBody Customer customer(){
            // ...
          }
        </CODE>

        Possible solutions:
         - build an interceptor and some AOP to look at the controller classes?


     */

     // test
    // todo we should have a way of supporting the @JsonpCallback annotation at the method level
    // that tells the controller which
     @RequestMapping(method = RequestMethod.GET,  value = "/test")
     @ResponseBody
     @JsonpCallback("cb")
     public String  test ( @RequestParam String cb ){
         System.out.println( "callback = "+ cb );
         return "Hello world!" ;
     }


    @RequestMapping(method = RequestMethod.GET, value = "/{user}/{repo}/{branch}/{module}")
    @ResponseBody
    public String code(@PathVariable("user") String user,
                       @PathVariable("repo") String repo,
                       @PathVariable("branch") String branch,
                       @RequestParam("file") String file,
                       @JsonpCallback ("cb")  String callback) {


        return contentForGithubPage(user, repo, branch, file);

    }


    protected String encodeForJson(String content) {
        return content.replaceAll("\"", "\\\"");
    }

}
