package org.jenkinsci.plugins.StashBranchParameter;

import com.atlassian.stash.rest.client.api.entity.Branch;
import com.atlassian.stash.rest.client.api.entity.Page;
import com.atlassian.stash.rest.client.api.entity.Repository;
import com.atlassian.stash.rest.client.core.StashClientImpl;
import com.atlassian.stash.rest.client.httpclient.HttpClientConfig;
import com.atlassian.stash.rest.client.httpclient.HttpClientHttpExecutor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StashConnector
{
	private final StashClientImpl stashClient;

	public StashConnector(String stashApiUrl, String username, String password) throws MalformedURLException
	{
		URL url = new URL(stashApiUrl);
		HttpClientConfig httpClientConfig = new HttpClientConfig(url, username, password);
		HttpClientHttpExecutor httpClientHttpExecutor = new HttpClientHttpExecutor(httpClientConfig);
		stashClient = new StashClientImpl(httpClientHttpExecutor);
	}

	public List<String> getBranches(final String project, final String repo)
	{
		List<Branch> branches = new ResourceDepletor<Branch>()
		{
			@Override
			protected Page<Branch> doPageCall(Integer nextPageStart)
			{
				return stashClient.getRepositoryBranches(project, repo, null, 0, 1000);

			}
		}.getAllValues();

		List<String> branchIds = new ArrayList<String>();
		for (Branch branch : branches)
		{
			branchIds.add(branch.getDisplayId());
		}
		return branchIds;
	}

	public List<String> getTags(final String project, final String repo)
	{
//		List<Tag> branches = new ResourceDepletor<Tag>()
//		{
//			@Override
//			protected Page<Tag> doPageCall(Integer nextPageStart)
//			{
//				return stashClient.getRepositoryTags(project, repo, null, 0, 1000);
//
//			}
//		}.getAllValues();
//
//		List<String> branchIds = new ArrayList<String>();
//		for (Tag branch : branches)
//		{
//			branchIds.add("tags/"+branch.getDisplayId());
//		}
//		return branchIds;
		return Collections.emptyList();
	}

	public Map<String, List<String>> getRepositories()
	{
		List<Repository> repositories = new ResourceDepletor<Repository>()
		{
			@Override
			protected Page<Repository> doPageCall(Integer nextPageStart)
			{
				return stashClient.getRepositories(null, null, nextPageStart, 1000);
			}
		}.getAllValues();

		Map<String, List<String>> map = new TreeMap<String, List<String>>();
		for (Repository repository : repositories)
		{
			addToMap(map, repository.getProject().getKey(), repository.getSlug());
		}
		return map;
	}

	private void addToMap(Map<String, List<String>> map, String key, String value)
	{
		if (!map.containsKey(key))
		{
			map.put(key, new LinkedList<String>());
		}
		map.get(key).add(value);
	}

	private abstract class ResourceDepletor<X>
	{
		List<X> getAllValues()
		{
			List<X> values = new ArrayList<X>();
			Page<X> pages = new Page<X>(0, 0, false, 0, 0, Collections.<X>emptyList());

			do
			{
				pages = doPageCall(pages.getNextPageStart());
				values.addAll(pages.getValues());
			}
			while (!pages.isLastPage());

			return values;
		}

		protected abstract Page<X> doPageCall(Integer nextPageStart);
	}
}
