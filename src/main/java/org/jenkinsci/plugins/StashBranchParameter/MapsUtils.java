package org.jenkinsci.plugins.StashBranchParameter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MapsUtils
{
	public static Map<String, Map<String, String>> groupMap(List<String> entries)
	{

		Map<String, Map<String, String>> result = new TreeMap<String, Map<String, String>>();

		for (String entry : entries)
		{
			if (entry.contains("/"))
			{
				String group = entry.substring(0, entry.indexOf("/"));
				String name = entry.substring(entry.indexOf("/") + 1);
				addToGroup(result, group, entry, name);
			}
			else
			{
				addToGroup(result, "branch", entry, entry);
			}
		}
		return result;
	}

	private static void addToGroup(Map<String, Map<String, String>> result, String group, String key, String name)
	{
		if (!result.containsKey(group))
		{
			result.put(group, new TreeMap<String, String>());
		}
		result.get(group).put(key, name);
	}

}
