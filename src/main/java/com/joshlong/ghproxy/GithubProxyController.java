package com.joshlong.ghproxy;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Josh Long
 */
@Controller
public class GithubProxyController {

    private RestTemplate restTemplate = new RestTemplate();

    private String urlPath = "https://raw.github.com/{user}/{repo}/{branch}/code";

    protected String contentForGithubPage(String user, String repos, String branch, String filePath) {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("user", user);
        vars.put("repo", repos);
        vars.put("branch", branch);
//        vars.put("file", filePath);
        ResponseEntity<String> code = restTemplate.getForEntity(
                urlPath + filePath(filePath), String.class, vars);
        return code.getBody();
    }

    private String filePath(String fp) {
        if (!fp.startsWith("/"))
            fp = "/" + fp;
        return fp;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{user}/{repo}/{branch}/{module}")
    public @ResponseBody String  code(@PathVariable("user") String user,
                                       @PathVariable("repo") String repo,
                                       @PathVariable("branch") String branch,
                                       @RequestParam("file") String file,
                                       @RequestParam(value = "callback", required = false) String callback) {


        String response;

        String content = contentForGithubPage(user, repo, branch, file);

        if (StringUtils.hasText(callback)) {
            response = callback + "(\"" + encodeForJson(content) + "\")";
        } else {
            response = content;
        }
        return response ; //new ResponseEntity<String>(response, HttpStatus.OK);
    }

    protected String encodeForJson ( String content ){
     return content.replaceAll("\"", "\\\"") ;
    }

    public static void main(String[] args) throws Throwable {
        //https://github.com/joshlong/the-spring-tutorial/blob/tut_web/code/web/src/main/java/org/springsource/examples/spring31/web/config/servlet/CrmWebApplicationInitializer.java
        GithubProxyController githubProxyController = new GithubProxyController();
        String branchCode = githubProxyController.contentForGithubPage("joshlong", "the-spring-tutorial", "tut_web",
                "web/src/main/java/org/springsource/examples/spring31/web/config/servlet/CrmWebApplicationInitializer.java");
        System.out.println(branchCode);
    }


/*
    @RequestMapping(value = "/crm/signin.html", method = RequestMethod.GET)
    public String showSignInPage(Model model, @RequestParam(value = "error", required = false, defaultValue = "false") String err) {

    }*/


}
