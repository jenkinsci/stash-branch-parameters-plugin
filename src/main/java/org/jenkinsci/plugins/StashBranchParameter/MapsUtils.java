package org.jenkinsci.plugins.StashBranchParameter;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MapsUtils
{
	public static Map<String, Map<String, String>> groupMap(Map<String, String> map)
	{
		Set<Map.Entry<String, String>> entries = map.entrySet();

		Map<String, Map<String, String>> result = new TreeMap<>();

		for (Map.Entry<String, String> entry : entries)
		{
			if (entry.getValue().contains("/"))
			{
				String group = entry.getValue().substring(0, entry.getValue().indexOf("/"));
				String name = entry.getValue().substring(entry.getValue().indexOf("/") + 1);
				addToGroup(result, group, entry.getKey(), name);
			}
			else
			{
				addToGroup(result, "branch", entry.getKey(), entry.getValue());
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
