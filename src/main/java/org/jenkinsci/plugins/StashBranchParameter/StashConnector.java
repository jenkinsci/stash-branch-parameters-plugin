package org.jenkinsci.plugins.StashBranchParameter;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class StashConnector
{
	private final String username;
	private final String password;
	private final URL url;
	private HttpHost target;

    public StashConnector(String stashApiUrl, String username, String password) throws MalformedURLException
	{
		this.username = username;
		this.password = password;
		url = new URL(stashApiUrl);
		target = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
	}

	public Map<String, String> getBranches(String project, String repo, String branchNameRegex, String branchNameFilter)
	{
		if (branchNameRegex.equals("^$")) {
			return new TreeMap<String, String>();
		}

		String params = "?orderBy=ALPHABETICAL&limit=1000";
		if (branchNameFilter != null && branchNameFilter.equals("")) {
			try {
				params += "&filterText=" + URLEncoder.encode(branchNameFilter, "UTF-8");
			} catch (Exception ignored) {
			}
		}

		String path = getBranchesPath(project, repo);
		List<JSONObject> allJsonPages =
                fetchAllAvailableJsonPages(path, params);
		Map<String, String> map = new TreeMap<>();
		Pattern pattern = compile(branchNameRegex);

        for (JSONObject json : allJsonPages){
            if (json.has("values"))
            {
                JSONArray values = json.getJSONArray("values");
                for (Object object : values)
                {
                    if (object instanceof JSONObject)
                    {
                        JSONObject branch = (JSONObject) object;
                        if (branch.has("displayId"))
                        {
                            String branchName = branch.getString("displayId");
                            if (pattern != null && !pattern.matcher(branchName).matches())
                            {
                                continue;
                            }
                            map.put(branchName, branchName);
                        }
                    }
                }
            }
        }
        return map;
	}

	public Map<String, String> getTags(String project, String repo, String tagNameRegex, String tagFilterText)
	{
		if (tagNameRegex.equals("^$")) {
			return new TreeMap<String, String>();
		}

		String params = "?orderBy=ALPHABETICAL&limit=1000";
		if (tagFilterText != null && tagFilterText.equals("")) {
			try {
				params += "&filterText=" + URLEncoder.encode(tagFilterText, "UTF-8");
			} catch (Exception ignored) {
			}
		}

		String path = getTagsPath(project, repo);
        List<JSONObject> allJsonPages =
                fetchAllAvailableJsonPages(path, params);
        Map<String, String> map = new TreeMap<>();
        Pattern pattern = compile(tagNameRegex);
        for (JSONObject json : allJsonPages){

            if (json.has("values"))
            {
                JSONArray values = json.getJSONArray("values");

                for (Object object : values)
                {
                    if (object instanceof JSONObject)
                    {
                        JSONObject branch = (JSONObject) object;
                        if (branch.has("displayId"))
                        {
                            String tagName = branch.getString("displayId");
                            if (pattern != null && !pattern.matcher(tagName).matches())
                            {
                                continue;
                            }
                            String value = "tags/".concat(tagName);
                            map.put(value,value);
                        }
                    }
                }
            }
        }
		return map;
	}

	public List<String> getProjects()
	{
		String path = getProjectsPath();
        List<JSONObject> allJsonPages =
                fetchAllAvailableJsonPages(path, "?orderBy=ALPHABETICAL&limit=1000");
		List<String> list = new LinkedList<>();
        for (JSONObject json : allJsonPages){
            if (json.has("values"))
            {
                JSONArray values = json.getJSONArray("values");
                for (Object object : values)
                {
                    if (object instanceof JSONObject)
                    {
                        JSONObject project = (JSONObject) object;
                        if (project.has("key"))
                        {
                            list.add(project.getString("key"));
                        }
                    }
                }
            }
        }
		return list;
	}

	public Map<String, List<String>> getRepositories()
	{
		String path = getRepositoriesPath();
        List<JSONObject> allJsonPages =
                fetchAllAvailableJsonPages(path, "?orderBy=ALPHABETICAL&limit=1000");
		Map<String, List<String>> map = new TreeMap<>();
        for (JSONObject json : allJsonPages){
            if (json.has("values"))
            {
                JSONArray values = json.getJSONArray("values");
                for (Object object : values)
                {
                    if (object instanceof JSONObject)
                    {
                        JSONObject repo = (JSONObject) object;
                        JSONObject project = repo.getJSONObject("project");
                        addToMap(map, project.getString("key"), repo.getString("slug"));
                    }
                }
            }
        }
		return map;
	}

    private synchronized List<JSONObject> fetchAllAvailableJsonPages(String path, String params)
    {
        List<JSONObject> jsonPages = new ArrayList<>();
        String startPath = path;
		int nextPageStart = 0;
		JSONObject currentJson;
        do
		{
			path = startPath + params +"&start=" + nextPageStart;
			currentJson = getJson(path);
			jsonPages.add(currentJson);
			if(currentJson.has("isLastPage"))
			{
				if (!currentJson.getBoolean("isLastPage"))
				{
					nextPageStart = currentJson.getInt("nextPageStart");
				}
			}
        }while(!currentJson.getBoolean("isLastPage"));

        return jsonPages;
    }

	private synchronized JSONObject getJson(String path)
	{
		try
		{
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(target.getHostName(), target.getPort()), new UsernamePasswordCredentials(username, password));

            try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build()) {
                AuthCache authCache = new BasicAuthCache();
                BasicScheme basicAuth = new BasicScheme();
                authCache.put(target, basicAuth);
                HttpClientContext localContext = HttpClientContext.create();
                localContext.setAuthCache(authCache);
                HttpGet httpget = new HttpGet(path);

                try (CloseableHttpResponse response = httpClient.execute(target, httpget, localContext)) {
                    HttpEntity entity = response.getEntity();
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(entity.getContent(), writer, "UTF-8");

                    return JSONObject.fromObject(writer.toString());
                }
            }
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

    private void addToMap(Map<String, List<String>> map, String key, String value)
	{
		if (!map.containsKey(key))
		{
			map.put(key, new LinkedList<String>());
		}
		map.get(key).add(value);
	}

	private String getRepositoriesPath()
	{
		return url.getPath().concat("/repos");
	}

	private String getProjectsPath()
	{
		return url.getPath().concat("/projects");
	}

	private String getRepositoriesPath(String project)
	{
		return getProjectsPath().concat("/").concat(project).concat("/repos");
	}

	private String getBranchesPath(String project, String repo)
	{
		return getRepositoriesPath(project).concat("/").concat(repo).concat("/branches");
	}

	private String getTagsPath(String project, String repo)
	{
		return getRepositoriesPath(project).concat("/").concat(repo).concat("/tags");
	}

	private Pattern compile(String regex) {
		if (!StringUtils.isEmpty(regex)) {
			return Pattern.compile(regex);
		}
		return null;
	}
}
