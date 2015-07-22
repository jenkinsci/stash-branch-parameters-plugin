package org.jenkinsci.plugins.StashBranchParameter;

import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.ParameterDefinition;
import hudson.model.StringParameterValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Created by erwin on 13/03/14.
 */
public class StashBranchParameterDefinition extends ParameterDefinition {

	private static final Logger LOGGER = Logger
			.getLogger(StashBranchParameterDefinition.class.getName());

	private String repository;
	private String defaultValue;
	private final List<String> branchFilters;

	/*
	 *
	 */
	@DataBoundConstructor
	public StashBranchParameterDefinition(final String name,
			final String description, final String repository,
			final String defaultValue, final String branchFilters) {
		super(name, description);
		this.repository = repository;
		this.defaultValue = defaultValue;
		this.branchFilters = new ArrayList<String>();
		// Strip whitespace
		for (final String filter : branchFilters.split(",")) {
			this.branchFilters.add(filter.trim());
		}

	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(final String repository) {
		this.repository = repository;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(final String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.ParameterDefinition#createValue(org.kohsuke.stapler.
	 * StaplerRequest, net.sf.json.JSONObject)
	 */
	@Override
	public ParameterValue createValue(final StaplerRequest staplerRequest,
			final JSONObject jsonObject) {
		final String value = jsonObject.getString("value");
		return new StringParameterValue(this.getName(), value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.ParameterDefinition#createValue(org.kohsuke.stapler.
	 * StaplerRequest)
	 */
	@Override
	public ParameterValue createValue(final StaplerRequest staplerRequest) {
		final String[] parameterValues = staplerRequest
				.getParameterValues(getName());
		final String value = parameterValues[0];
		return new StringParameterValue(this.getName(), value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.ParameterDefinition#getDefaultParameterValue()
	 */
	@Override
	public ParameterValue getDefaultParameterValue() {
		return new StringParameterValue(this.getName(), defaultValue);
	}

	public Map<String, Map<String, String>> getDefaultValueMap()
			throws IOException {
		return computeDefaultValueMap();
	}

	private Map<String, Map<String, String>> computeDefaultValueMap()
			throws IOException {
		final String project = repository.split("/")[0];
		final String repo = repository.split("/")[1];
		final StashConnector connector = new StashConnector(getDescriptor()
				.getStashApiUrl(), getDescriptor().getUsername(),
				getDescriptor().getPassword());

		final Map<String, String> map;
		if (branchFilters.isEmpty()) {
			map = connector.getBranches(project, repo);
		} else {
			map = connector.getFilteredBranches(project, repo, branchFilters);
		}
		if (StringUtils.isNotBlank(defaultValue)) {
			map.put(defaultValue, defaultValue);
		}
		map.putAll(connector.getTags(project, repo));

		final Map<String, Map<String, String>> stringMapMap = MapsUtils
				.groupMap(map);
		return stringMapMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see hudson.model.ParameterDefinition#getDescriptor()
	 */
	@Override
	public StashBranchParameterDescriptorImpl getDescriptor() {
		return (StashBranchParameterDescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static class StashBranchParameterDescriptorImpl extends
			StashBranchParameterDescriptor {

	}

}
