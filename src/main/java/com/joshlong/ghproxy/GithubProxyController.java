package com.joshlong.ghproxy;

import com.joshlong.ghproxy.jsonp.JsonpContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple controller that simply forwards the request to the
 * appropriate <A href="http://github.com">GitHub</A> repository.
 *
 * @author Josh Long
 */
@Controller
public class GithubProxyController {

    private RestTemplate restTemplate = new RestTemplate();
    private String urlForCode = "https://raw.github.com/{user}/{repo}/{branch}/code";
    private String urlForGist = "https://gist.github.com/{user}/{gist}/raw/";

    private Map<String, String> cache = new ConcurrentHashMap<String, String>();

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



    @RequestMapping(method = RequestMethod.GET, value = "/gist/{user}/{gist}")
    @ResponseBody
    public String gist(final @PathVariable("user") String user,
                       final @PathVariable("gist") String gist,
                       @RequestParam("callback") String callback,
                       JsonpContext context) throws Throwable {

        context.setJsonPadding(StringUtils.hasText(callback) ? callback : "callback");

        return fromCache(user + gist,
                new CodeLoader() {
                    @Override
                    public String codeFor(String k) {
                        return contentForGithubGist(user, gist);
                    }
                });
    }

    interface CodeLoader {
        String codeFor(String k);
    }

    protected String fromCache(String k, CodeLoader codeLoader) throws Exception {
        if (cache.containsKey(k)) {
            return cache.get(k);
        } else {
            String x = codeLoader.codeFor(k);
            cache.put(k, x);
            return x;
        }
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/cache/dump")
    public String contents() {
        return cache.toString();
    }

    @ResponseStatus(value = HttpStatus.OK)
    @RequestMapping(method = RequestMethod.GET, value = "/cache/{key}")
    public void invalidate( @PathVariable("key") String k) {
        cache.remove(k)  ;
    }


    @ResponseStatus(value = HttpStatus.OK)
    @RequestMapping(method = RequestMethod.GET, value = "/cache/invalidate")
    public void invalidate() {
        cache.clear();
    }



    @RequestMapping(method = RequestMethod.GET, value = "/{user}/{repo}/{branch}/{module}")
    @ResponseBody
    public String code(final @PathVariable("user") String user,
                       final @PathVariable("repo") String repo,
                       final @PathVariable("branch") String branch,
                       final @RequestParam("file") String file,
                       @RequestParam String callback,
                       JsonpContext context) throws Throwable {

        context.setJsonPadding(callback);

        return this.fromCache(user + repo + branch + file, new CodeLoader() {
            @Override
            public String codeFor(String k) {
                return contentForGithubPage(user, repo, branch, file);
            }
        });
    }

    private String filePath(String fp) {
        if (!fp.startsWith("/"))
            fp = "/" + fp;
        return fp;
    }


}
