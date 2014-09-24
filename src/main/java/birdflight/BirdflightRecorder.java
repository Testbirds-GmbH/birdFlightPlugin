package birdflight;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.*;
import hudson.util.CopyOnWriteList;
import hudson.util.RunList;
import hudson.util.Secret;

import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.Hudson;

import java.util.*;

import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.kohsuke.stapler.StaplerRequest;

import birdflight.Messages;

public class BirdflightRecorder extends Recorder {

	private String version;
	private String compatibility;
	private String distribution;
	private Boolean isPublic;
	private String packIdentifier;
        private String buildIdentifier;

	public String getVersion() {
		return version;
	}

	public String getCompatibility() {
		return compatibility;
	}

	public String getDistribution() {
		return distribution;
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public String getPackIdentifier() {
		return packIdentifier;
	}
        
        public String getBuildIdentifier() {
		return buildIdentifier;
	}

	private String tokenPairName;

	public String getTokenPairName() {
		return this.tokenPairName;
	}

	private Secret apiToken;

	@Deprecated
	public Secret getApiToken() {
		return this.apiToken;
	}

	private Secret teamToken;

	@Deprecated
	public Secret getTeamToken() {
		return this.teamToken;
	}

	private Boolean notifyTeam;

	public Boolean getNotifyTeam() {
		return this.notifyTeam;
	}

	private String buildNotes;

	public String getBuildNotes() {
		return this.buildNotes;
	}

	private boolean appendChangelog;

	public boolean getAppendChangelog() {
		return this.appendChangelog;
	}

	/**
	 * Comma- or space-separated list of patterns of files/directories to be
	 * archived. The variable hasn't been renamed yet for compatibility reasons
	 */
	private String filePath;

	public String getFilePath() {
		return this.filePath;
	}

	private String dsymPath;

	public String getDsymPath() {
		return this.dsymPath;
	}

	private String lists;

	public String getLists() {
		return this.lists;
	}

	private Boolean replace;

	public Boolean getReplace() {
		return this.replace;
	}

	private String proxyHost;

	@Deprecated
	public String getProxyHost() {
		return proxyHost;
	}

	private String proxyUser;

	@Deprecated
	public String getProxyUser() {
		return proxyUser;
	}

	private String proxyPass;

	@Deprecated
	public String getProxyPass() {
		return proxyPass;
	}

	private int proxyPort;

	@Deprecated
	public int getProxyPort() {
		return proxyPort;
	}

	private Boolean debug;

	public Boolean getDebug() {
		return this.debug;
	}

	@DataBoundConstructor
	public BirdflightRecorder(String tokenPairName, String buildIdentifier, Secret apiToken,
			Secret teamToken, String filePath, String dsymPath,
			String packIdentifier, String version, String buildNotes,
			String compatibility, String distribution, Boolean isPublic) {
		this.tokenPairName = tokenPairName;
		this.apiToken = apiToken;
		this.teamToken = teamToken;
		this.buildNotes = buildNotes;
		this.filePath = filePath;
		this.dsymPath = dsymPath;
		this.packIdentifier = packIdentifier;
                this.buildIdentifier = buildIdentifier;
		this.version = version;
		this.compatibility = compatibility;
		this.isPublic = isPublic;
		this.distribution = distribution;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			final BuildListener listener) {
		if (build.getResult().isWorseOrEqualTo(Result.FAILURE))
			return false;

		listener.getLogger().println(
				Messages.birdflightRecorder_InfoUploading());

		try {
			EnvVars vars = build.getEnvironment(listener);

			String workspace = vars.expand("$WORKSPACE");

			List<BirdflightUploader.UploadRequest> urList = new ArrayList<BirdflightUploader.UploadRequest>();
			BirdflightUploader.UploadRequest ur = createPartialUploadRequest(
					vars, build);
			BirdflightRemoteRecorder remoteRecorder = new BirdflightRemoteRecorder(
					workspace, ur, listener);
			launcher.getChannel().call(remoteRecorder);
		} catch (Throwable e) {
			listener.getLogger().println(e);
			e.printStackTrace(listener.getLogger());
			return false;
		}

		return true;
	}

	private BirdflightUploader.UploadRequest createPartialUploadRequest(
			EnvVars vars, AbstractBuild<?, ?> build) {
		BirdflightUploader.UploadRequest ur = new BirdflightUploader.UploadRequest();
		TokenPair tokenPair = getTokenPair(getTokenPairName());
		ur.filePaths = vars.expand(StringUtils.trim(getFilePath()));
		ur.dsymPath = vars.expand(getDsymPath());
		ur.apiToken = vars.expand(Secret.toString(tokenPair.getApiToken()));
		ur.buildNotes = createBuildNotes(vars.expand(buildNotes),
				build.getChangeSet());
		ur.version = vars.expand(version);
		ur.distribution = vars.expand(distribution);
		ur.compatibility = vars.expand(compatibility);
		ur.isPublic = Boolean.parseBoolean(vars.expand(isPublic.toString()));
		ur.packIdentifier = vars.expand(packIdentifier);
                ur.buildIdentifier = vars.expand(buildIdentifier);
		ur.lists = vars.expand(lists);
		// ur.notifyTeam = notifyTeam;
		ProxyConfiguration proxy = getProxy();
//		ur.proxyHost = proxy.name;
//		ur.proxyPass = proxy.getPassword();
//		ur.proxyPort = proxy.port;
//		ur.proxyUser = proxy.getUserName();
		ur.replace = replace;
		// ur.teamToken =
		// vars.expand(Secret.toString(tokenPair.getTeamToken()));
		ur.debug = debug;
		return ur;
	}

	private ProxyConfiguration getProxy() {
		ProxyConfiguration proxy;
		if (Hudson.getInstance() != null && Hudson.getInstance().proxy != null) {
			proxy = Hudson.getInstance().proxy;
		} else if (proxyHost != null && proxyPort > 0) {
			// backward compatibility for pre-1.3.7 configurations
			proxy = new ProxyConfiguration(proxyHost, proxyPort, proxyUser,
					proxyPass);
		} else {
			proxy = new ProxyConfiguration("", 0, "", "");
		}
		return proxy;
	}

	// Append the changelog if we should and can
	private String createBuildNotes(String buildNotes,
			final ChangeLogSet<?> changeSet) {
		if (appendChangelog) {
			StringBuilder stringBuilder = new StringBuilder();

			// Show the build notes first
			stringBuilder.append(buildNotes);

			// Then append the changelog
			stringBuilder
					.append("\n\n")
					.append(changeSet.isEmptySet() ? Messages
							.birdflightRecorder_EmptyChangeSet() : Messages
							.birdflightRecorder_Changelog()).append("\n");

			int entryNumber = 1;

			for (Entry entry : changeSet) {
				stringBuilder.append("\n").append(entryNumber).append(". ");
				stringBuilder.append(entry.getMsg()).append(" \u2014 ")
						.append(entry.getAuthor());

				entryNumber++;
			}
			buildNotes = stringBuilder.toString();
		}
		return buildNotes;
	}

	private TokenPair getTokenPair(String tokenPairName) {
		for (TokenPair tokenPair : getDescriptor().getTokenPairs()) {
			if (tokenPair.getTokenPairName().equals(tokenPairName))
				return tokenPair;
		}

		if (getApiToken() != null && getTeamToken() != null)
			return new TokenPair("", getApiToken());

		String tokenPairNameForMessage = tokenPairName != null ? tokenPairName
				: "(null)";
		throw new MisconfiguredJobException(
				Messages._birdflightRecorder_TokenPairNotFound(tokenPairNameForMessage));
	}

	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> {
		private final CopyOnWriteList<TokenPair> tokenPairs = new CopyOnWriteList<TokenPair>();

		public DescriptorImpl() {
			super(BirdflightRecorder.class);
			load();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project
			// types
			return true;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json)
				throws FormException {
			tokenPairs.replaceBy(req.bindParametersToList(TokenPair.class,
					"tokenPair."));
			save();
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return Messages.birdflightRecorder_UploadLinkText();
		}

		public Iterable<TokenPair> getTokenPairs() {
			return tokenPairs;
		}
	}

	private static class EnvAction implements EnvironmentContributingAction {
		private transient Map<String, String> data = new HashMap<String, String>();

		private void add(String key, String value) {
			if (data == null)
				return;
			data.put(key, value);
		}

		public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
			if (data != null)
				env.putAll(data);
		}

		public String getIconFileName() {
			return null;
		}

		public String getDisplayName() {
			return null;
		}

		public String getUrlName() {
			return null;
		}
	}
}
