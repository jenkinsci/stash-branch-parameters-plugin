package org.jenkinsci.plugins.StashBranchParameter;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
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

/**
 * Created by erwin on 24/03/14.
 */
public class StashConnector {

	private static final Logger LOGGER = Logger.getLogger(StashConnector.class
			.getName());

	private final String stashApiUrl;
	private final String username;
	private final String password;
	private final URL url;
	private CloseableHttpClient httpclient = null;
	private HttpHost target = null;
	private HttpClientContext localContext;

	public StashConnector(final String stashApiUrl, final String username,
			final String password) throws MalformedURLException {

		this.stashApiUrl = stashApiUrl;
		this.username = username;
		this.password = password;
		url = new URL(stashApiUrl);
		target = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
	}

	public Map<String, String> getBranches(final String project,
			final String repo) {

		String path = getBranchesPath(project, repo);
		path = path.concat("?orderBy=ALPHABETICAL&limit=1000");

		final JSONObject json = getJson(path);
		final Map<String, String> map = new TreeMap<String, String>();
		if (json.has("values")) {
			final JSONArray values = json.getJSONArray("values");
			final Iterator<JSONObject> iterator = values.iterator();
			while (iterator.hasNext()) {
				final JSONObject branch = iterator.next();
				if (branch.has("displayId")) {
					map.put(branch.getString("displayId"),
							branch.getString("displayId"));
				}
			}
		}
		return map;
	}

	/**
	 *
	 * @param project
	 *            Name of the project
	 * @param repo
	 *            Name of the repo
	 * @param branchFilters
	 *            List of text filter. Each filter will be applied separatly
	 * @return Map of branch matching with a filter
	 */
	public Map<String, String> getFilteredBranches(final String project,
			final String repo, final List<String> branchFilters) {

		final Map<String, String> map = new TreeMap<String, String>();
		for (final String filter : branchFilters) {
			final String path = getFilterBranchPath(project, repo, filter);
			path.concat("&orderBy=ALPHABETICAL&limit=1000");
			final JSONObject json = getJson(path);
			if (json.has("values")) {
				final JSONArray values = json.getJSONArray("values");
				final Iterator<JSONObject> iterator = values.iterator();
				while (iterator.hasNext()) {
					final JSONObject branch = iterator.next();
					if (branch.has("displayId")) {
						map.put(branch.getString("displayId"),
								branch.getString("displayId"));
					}
				}
			}
		}
		return map;
	}

	public Map<String, String> getTags(final String project, final String repo) {

		String path = getTagsPath(project, repo);
		path = path.concat("?orderBy=ALPHABETICAL&limit=1000");

		final JSONObject json = getJson(path);
		final Map<String, String> map = new TreeMap<String, String>();
		if (json.has("values")) {
			final JSONArray values = json.getJSONArray("values");
			final Iterator<JSONObject> iterator = values.iterator();
			while (iterator.hasNext()) {
				final JSONObject branch = iterator.next();
				if (branch.has("displayId")) {
					final String value = "tags/".concat(branch
							.getString("displayId"));
					map.put(value, value);
				}
			}
		}
		return map;
	}

	public List<String> getProjects() {
		String path = getProjectsPath();
		path = path.concat("?orderBy=ALPHABETICAL&limit=1000");
		final JSONObject json = getJson(path);

		final List<String> list = new LinkedList<String>();
		if (json.has("values")) {
			final JSONArray values = json.getJSONArray("values");
			final Iterator<JSONObject> iterator = values.iterator();
			while (iterator.hasNext()) {
				final JSONObject project = iterator.next();
				if (project.has("key")) {
					list.add(project.getString("key"));
				}
			}
		}
		return list;
	}

	public Map<String, List<String>> getRepositories() {
		String path = getRepositoriesPath();
		path = path.concat("?orderBy=ALPHABETICAL&limit=1000");
		final JSONObject json = getJson(path);
		final Map<String, List<String>> map = new TreeMap<String, List<String>>();
		if (json.has("values")) {
			final JSONArray values = json.getJSONArray("values");
			final Iterator<JSONObject> iterator = values.iterator();
			while (iterator.hasNext()) {
				final JSONObject repo = iterator.next();
				final JSONObject project = repo.getJSONObject("project");
				addToMap(map, project.getString("key"), repo.getString("slug"));
			}
		}
		return map;
	}

	private synchronized JSONObject getJson(final String path) {
		try {
			initConnections();
			final HttpGet httpget = new HttpGet(path);

			final CloseableHttpResponse response = httpclient.execute(target,
					httpget, localContext);
			try {
				final HttpEntity entity = response.getEntity();
				final StringWriter writer = new StringWriter();
				IOUtils.copy(entity.getContent(), writer);

				return JSONObject.fromObject(writer.toString());
			} finally {
				response.close();
			}

		} catch (final IOException e) {
			throw new RuntimeException("Error while communicating with Stash",
					e);
		} finally {
			if (httpclient != null) {
				try {
					httpclient.close();
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void initConnections() {
		final CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
				new AuthScope(target.getHostName(), target.getPort()),
				new UsernamePasswordCredentials(username, password));
		httpclient = HttpClients.custom()
				.setDefaultCredentialsProvider(credsProvider).build();
		final AuthCache authCache = new BasicAuthCache();
		final BasicScheme basicAuth = new BasicScheme();
		authCache.put(target, basicAuth);
		localContext = HttpClientContext.create();
		localContext.setAuthCache(authCache);
	}

	private void addToMap(final Map<String, List<String>> map,
			final String key, final String value) {
		if (!map.containsKey(key)) {
			map.put(key, new LinkedList<String>());
		}
		map.get(key).add(value);
	}

	private String getRepositoriesPath() {
		return url.getPath().concat("/repos");
	}

	private String getProjectsPath() {
		return url.getPath().concat("/projects");
	}

	private String getRepositoriesPath(final String project) {
		return getProjectsPath().concat("/").concat(project).concat("/repos");
	}

	private String getBranchesPath(final String project, final String repo) {
		return getRepositoriesPath(project).concat("/").concat(repo)
				.concat("/branches");
	}

	private String getFilterBranchPath(final String project, final String repo,
			final String filter) {
		return getRepositoriesPath(project).concat("/").concat(repo)
				.concat("/branches").concat("?filterText=").concat(filter);
	}

	private String getTagsPath(final String project, final String repo) {
		return getRepositoriesPath(project).concat("/").concat(repo)
				.concat("/tags");
	}
}
