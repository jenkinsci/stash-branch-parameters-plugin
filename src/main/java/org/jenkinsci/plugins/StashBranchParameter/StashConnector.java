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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class StashConnector
{
	private String username;
	private String password;
	private URL url;
	private CloseableHttpClient httpclient = null;
	private HttpHost target = null;
	private HttpClientContext localContext;

	public StashConnector(String stashApiUrl, String username, String password) throws MalformedURLException
	{
		this.username = username;
		this.password = password;
		url = new URL(stashApiUrl);
		target = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());

	}

	public Map<String, String> getBranches(String project, String repo, String branchNameRegex)
	{
		String path = getBranchesPath(project, repo);
		path = path.concat("?orderBy=ALPHABETICAL&limit=1000");

		JSONObject json = getJson(path);
		Map<String, String> map = new TreeMap<String, String>();
		if (json.has("values"))
		{
			Pattern pattern = compile(branchNameRegex);
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
		return map;
	}

	public Map<String, String> getTags(String project, String repo, String tagNameRegex)
	{
		String path = getTagsPath(project, repo);
		path = path.concat("?orderBy=ALPHABETICAL&limit=1000");

		JSONObject json = getJson(path);
		Map<String, String> map = new TreeMap<String, String>();
		if (json.has("values"))
		{
			Pattern pattern = compile(tagNameRegex);
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
		return map;
	}

	public List<String> getProjects()
	{

		String path = getProjectsPath();
		path = path.concat("?orderBy=ALPHABETICAL&limit=1000");
		JSONObject json = getJson(path);

		List<String> list = new LinkedList<String>();
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
		return list;
	}

	public Map<String, List<String>> getRepositories()
	{

		String path = getRepositoriesPath();
		path = path.concat("?orderBy=ALPHABETICAL&limit=1000");
		JSONObject json = getJson(path);
		Map<String, List<String>> map = new TreeMap<String, List<String>>();
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
		return map;
	}

	private synchronized JSONObject getJson(String path)
	{
		try
		{
			initConnections();
			HttpGet httpget = new HttpGet(path);

			CloseableHttpResponse response = httpclient.execute(target, httpget, localContext);
			try
			{
				HttpEntity entity = response.getEntity();
				StringWriter writer = new StringWriter();
				IOUtils.copy(entity.getContent(), writer);

				return JSONObject.fromObject(writer.toString());
			}
			finally
			{
				response.close();
			}

		}
		catch (IOException e)
		{
			throw new RuntimeException();
		}
		finally
		{
			if (httpclient != null)
			{
				try
				{
					httpclient.close();
				}
				catch (IOException e)
				{
					throw new RuntimeException();
				}
			}
		}
	}

	private void initConnections()
	{
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(target.getHostName(), target.getPort()), new UsernamePasswordCredentials(username, password));
		httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(target, basicAuth);
		localContext = HttpClientContext.create();
		localContext.setAuthCache(authCache);
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
