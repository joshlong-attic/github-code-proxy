package com.joshlong.ghproxy;

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
    public String gist(@PathVariable("user") String user,
                       @PathVariable("gist") String gist,
                       @JsonpCallback  String callback ) {


        return contentForGithubGist(user, gist);

    }


    @RequestMapping(method = RequestMethod.GET, value = "/{user}/{repo}/{branch}/{module}")
    @ResponseBody
    public String code(@PathVariable("user") String user,
                       @PathVariable("repo") String repo,
                       @PathVariable("branch") String branch,
                       @RequestParam("file") String file,
                       @JsonpCallback String callback) {


        return contentForGithubPage(user, repo, branch, file);

    }


    protected String encodeForJson(String content) {
        return content.replaceAll("\"", "\\\"");
    }

}
