package org.springframework.cloud.stream.app.documentation.plugin;


import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 */
@Mojo(name = "generate-documentation", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ConfigurationMetadataDocumentationMojo extends AbstractMojo {

	private ClassLoaderExposingMetadataResolver metadataResolver = new ClassLoaderExposingMetadataResolver();

	@Parameter(defaultValue = "${project}")
	private MavenProject mavenProject;

	public void execute() throws MojoExecutionException {

		File readme = new File(mavenProject.getBasedir(), "README.adoc");
		if (!readme.exists()) {
			getLog().info(String.format("No README.adoc file found in %s, skipping", mavenProject.getBasedir()));
			return;
		}

		Artifact artifact = mavenProject.getArtifact();
		if (artifact.getFile() == null) {
			getLog().info(String.format("Project in %s does not produce a build artifact, skipping", mavenProject.getBasedir()));
			return;
		}

		File tmp = new File(readme.getPath() + ".tmp");
		try (PrintWriter out = new PrintWriter(tmp); BufferedReader reader = new BufferedReader(new FileReader(readme));) {

			String line = null;
			do {
				line = reader.readLine();
				out.println(line);
			}
			while (line != null && !line.startsWith("//tag::configuration-properties[]"));
			if (line == null) {
				getLog().info("No documentation section marker found");
				return;
			}

			ClassLoader classLoader = metadataResolver.appClassLoader(mavenProject);

			List<ConfigurationMetadataProperty> properties = metadataResolver.listProperties(new FileSystemResource(artifact.getFile()));
			Collections.sort(properties, new Comparator<ConfigurationMetadataProperty>() {

				@Override
				public int compare(ConfigurationMetadataProperty p1, ConfigurationMetadataProperty p2) {
					return p1.getId().compareTo(p2.getId());
				}
			});

			for (ConfigurationMetadataProperty property : properties) {
				getLog().debug("Documenting " + property.getId());
				out.println(asciidocFor(property, classLoader));
			}

			do {
				line = reader.readLine();
				// drop lines
			}
			while (!line.startsWith("//end::configuration-properties[]"));

			// Copy remaining lines, including //end::configuration-properties[]
			while (line != null) {
				out.println(line);
				line = reader.readLine();
			}
			if (classLoader instanceof Closeable) {
				((Closeable) classLoader).close();
			}
			getLog().info(String.format("Documented %d configuration properties", properties.size()));
			tmp.renameTo(readme);
		}
		catch (IOException e) {
			throw new MojoExecutionException("Error generating documentation", e);
		} finally {
			tmp.delete();
		}

	}

	private String asciidocFor(ConfigurationMetadataProperty property, ClassLoader classLoader) {
		return String.format("$$%s$$:: $$%s$$ *($$%s$$, default: `%s`%s)*",
				property.getId(),
				niceDescription(property),
				niceType(property),
				niceDefault(property),
				maybeHints(property, classLoader));
	}

	private String niceDescription(ConfigurationMetadataProperty property) {
		return property.getDescription() == null ? "<documentation missing>" : property.getDescription();
	}

	private CharSequence maybeHints(ConfigurationMetadataProperty property, ClassLoader classLoader) {
		String type = property.getType().replace('$', '.');
		if (ClassUtils.isPresent(type, classLoader)) {
			Class<?> clazz = ClassUtils.resolveClassName(type, classLoader);
			if (clazz.isEnum()) {
				return " possible values: `" + StringUtils.arrayToDelimitedString(clazz.getEnumConstants(), "`,`") + "`";
			}
		}
		return "";
	}

	private String niceDefault(ConfigurationMetadataProperty property) {
		if (property.getDefaultValue() == null) {
			return "<none>";
		} else if ("".equals(property.getDefaultValue())) {
			return "<empty string>";
		}
		return String.valueOf(property.getDefaultValue());
	}

	private String niceType(ConfigurationMetadataProperty property) {
		String type = property.getType();
		if (type == null) {
			return "<unknown>";
		}
		int lastDot = type.lastIndexOf('.');
		int lastDollar = type.lastIndexOf('$');
		return type.substring(Math.max(lastDot, lastDollar) + 1);
	}

	private static class ClassLoaderExposingMetadataResolver extends ApplicationConfigurationMetadataResolver {

		public ClassLoader appClassLoader(MavenProject mavenProject) throws MojoExecutionException {
			try {
				return createClassLoader(new ScatteredArchive(mavenProject));
			}
			catch (Exception e) {
				throw new MojoExecutionException("Error creating classloader for app", e);
			}
		}
	}

	/**
	 * An adapter to boot {@link Archive} that satisfies just enough of the API to craft a ClassLoader that
	 * "sees" all the properties that this Mojo tries to document.
	 *
	 * @author Eric Bottard
	 */
	private static class ScatteredArchive extends Archive {

		private final MavenProject mavenProject;

		private ScatteredArchive(MavenProject mavenProject) {

			this.mavenProject = mavenProject;
		}

		@Override
		public URL getUrl() throws MalformedURLException {
			return mavenProject.getArtifact().getFile().toURI().toURL();
		}

		@Override
		public Manifest getManifest() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<Entry> getEntries() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Archive> getNestedArchives(EntryFilter ignored) throws IOException {
			try {
				List<Archive> archives = new ArrayList<>(mavenProject.getRuntimeClasspathElements().size());
				for (String dep : mavenProject.getRuntimeClasspathElements()) {
					File file = new File(dep);
					archives.add(file.isDirectory() ? new ExplodedArchive(file) : new JarFileArchive(file));
				}
				return archives;
			}
			catch (DependencyResolutionRequiredException e) {
				throw new IOException("Could not create boot archive", e);
			}

		}

		@Override
		public Archive getFilteredArchive(EntryRenameFilter filter) throws IOException {
			throw new UnsupportedOperationException();
		}
	}
}
