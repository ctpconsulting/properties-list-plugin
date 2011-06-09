package com.ctp.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.resources.ResourcesMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.DefaultMavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;

import java.io.*;
import java.util.*;

/**
 * Goal which touches a timestamp file.
 *
 * @goal resourcesPrinting
 * @phase process-sources
 */
public class ResourcesPrintingMojo extends AbstractMojo {

    /**
     * The character encoding scheme to be applied when filtering resources.
     *
     * @parameter expression="${encoding}" default-value="${project.build.sourceEncoding}"
     */
    protected String encoding;

    /**
     * The output directory into which to copy the resources.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The list of resources we want to transfer.
     *
     * @parameter default-value="${project.resources}"
     * @required
     * @readonly
     */
    private List resources;

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The list of additional filter properties files to be used along with System and project
     * properties, which would be used for the filtering.
     * <br/>
     * See also: {@link ResourcesMojo#filters}.
     *
     * @parameter default-value="${project.build.filters}"
     * @readonly
     * @since 2.4
     */
    protected List buildFilters;

    /**
     * The list of extra filter properties files to be used along with System properties,
     * project properties, and filter properties files specified in the POM build/filters section,
     * which should be used for the filtering during the current mojo execution.
     * <br/>
     * Normally, these will be configured from a plugin's execution section, to provide a different
     * set of filters for a particular execution. For instance, starting in Maven 2.2.0, you have the
     * option of configuring executions with the id's <code>default-resources</code> and
     * <code>default-testResources</code> to supply different configurations for the two
     * different types of resources. By supplying <code>extraFilters</code> configurations, you
     * can separate which filters are used for which type of resource.
     *
     * @parameter
     */
    protected List filters;

    /**
     * If false, don't use the filters specified in the build/filters section of the POM when
     * processing resources in this mojo execution.
     * <br/>
     * See also: {@link ResourcesMojo#buildFilters} and {@link ResourcesMojo#filters}
     *
     * @parameter default-value="true"
     * @since 2.4
     */
    protected boolean useBuildFilters;

    /**
     * @component role="org.apache.maven.shared.filtering.MavenResourcesFiltering" role-hint="default"
     * @required
     */
    protected MavenResourcesFiltering mavenResourcesFiltering;

    /**
     * @parameter default-value="${session}"
     * @readonly
     * @required
     */
    protected MavenSession session;

    /**
     * Expression preceded with the String won't be interpolated
     * \${foo} will be replaced with ${foo}
     *
     * @parameter default-value="${maven.resources.escapeString}"
     * @since 2.3
     */
    protected String escapeString;

    /**
     * Overwrite existing files even if the destination files are newer.
     *
     * @parameter expression="${maven.resources.overwrite}" default-value="false"
     * @since 2.3
     */
    private boolean overwrite;

    /**
     * Copy any empty directories included in the Ressources.
     *
     * @parameter expression="${maven.resources.includeEmptyDirs}" default-value="false"
     * @since 2.3
     */
    protected boolean includeEmptyDirs;

    /**
     * Additionnal file extensions to not apply filtering (already defined are : jpg, jpeg, gif, bmp, png)
     *
     * @parameter
     * @since 2.3
     */
    protected List nonFilteredFileExtensions;

    /**
     * Whether to escape backslashes and colons in windows-style paths.
     *
     * @parameter expression="${maven.resources.escapeWindowsPaths}" default-value="true"
     * @since 2.4
     */
    protected boolean escapeWindowsPaths;

    /**
     * <p>
     * Set of delimiters for expressions to filter within the resources. These delimiters are specified in the
     * form 'beginToken*endToken'. If no '*' is given, the delimiter is assumed to be the same for start and end.
     * </p><p>
     * So, the default filtering delimiters might be specified as:
     * </p>
     * <pre>
     * &lt;delimiters&gt;
     *   &lt;delimiter&gt;${*}&lt/delimiter&gt;
     *   &lt;delimiter&gt;@&lt/delimiter&gt;
     * &lt;/delimiters&gt;
     * </pre>
     * <p>
     * Since the '@' delimiter is the same on both ends, we don't need to specify '@*@' (though we can).
     * </p>
     *
     * @parameter
     * @since 2.4
     */
    protected List delimiters;

    /**
     * @parameter default-value="true"
     * @since 2.4
     */
    protected boolean useDefaultDelimiters;

    /**
     * @component role="org.apache.maven.shared.filtering.MavenFileFilter" role-hint="default"
     */
    protected DefaultMavenFileFilter defaultMavenFileFilter;

    public void execute() throws MojoExecutionException {


            if (StringUtils.isEmpty(encoding) && isFilteringEnabled(getResources())) {
                getLog().warn(
                        "File encoding has not been set, using platform encoding " + ReaderFactory.FILE_ENCODING
                                + ", i.e. build is platform dependent!");
            }

            List filters = getCombinedFiltersList();

            MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(getResources(),
                    getOutputDirectory(),
                    project, encoding, filters,
                    Collections.EMPTY_LIST,
                    session);

            mavenResourcesExecution.setEscapeWindowsPaths(escapeWindowsPaths);

            // never include project build filters in this call, since we've already accounted for the POM build filters
            // above, in getCombinedFiltersList().
            mavenResourcesExecution.setInjectProjectBuildFilters(false);

            mavenResourcesExecution.setEscapeString(escapeString);
            mavenResourcesExecution.setOverwrite(overwrite);
            mavenResourcesExecution.setIncludeEmptyDirs(includeEmptyDirs);

            // if these are NOT set, just use the defaults, which are '${*}' and '@'.
            if (delimiters != null && !delimiters.isEmpty()) {
                LinkedHashSet delims = new LinkedHashSet();
                if (useDefaultDelimiters) {
                    delims.addAll(mavenResourcesExecution.getDelimiters());
                }

                for (Iterator dIt = delimiters.iterator(); dIt.hasNext(); ) {
                    String delim = (String) dIt.next();
                    if (delim == null) {
                        // FIXME: ${filter:*} could also trigger this condition. Need a better long-term solution.
                        delims.add("${*}");
                    } else {
                        delims.add(delim);
                    }
                }

                mavenResourcesExecution.setDelimiters(delims);
            }

            if (nonFilteredFileExtensions != null) {
                mavenResourcesExecution.setNonFilteredFileExtensions(nonFilteredFileExtensions);
            }
        //mavenResourcesFiltering.filterResources(mavenResourcesExecution);

        try {
            mavenResourcesExecution.setFilterWrappers(
                defaultMavenFileFilter.getDefaultFilterWrappers(mavenResourcesExecution)
            );
        } catch (MavenFilteringException e) {
            throw new RuntimeException(e);
        }

        File f = outputDirectory;

        if (!f.exists()) {
            f.mkdirs();
        }

        File file = new File(f, "filtered.txt");

        PrintWriter pw = null;

        try {
            pw = new PrintWriter(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }


        InputStreamReader r = null;
        for (Object filter: this.project.getFilters()) {
            try {
                r = new InputStreamReader(new FileInputStream(filter.toString()));
                System.out.println("FILE: " + filter.toString());
                pw.println("FILE: " + filter.toString());
                for (Object wrappers: mavenResourcesExecution.getFilterWrappers()) {
                    FileUtils.FilterWrapper w = (FileUtils.FilterWrapper) wrappers;
                    IOUtil.copy(w.getReader(r), pw);
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }


    }

    protected List getCombinedFiltersList() {
        if (filters == null || filters.isEmpty()) {
            return useBuildFilters ? buildFilters : null;
        } else {
            List result = new ArrayList();

            if (useBuildFilters && buildFilters != null && !buildFilters.isEmpty()) {
                result.addAll(buildFilters);
            }

            result.addAll(filters);

            return result;
        }
    }

    /**
     * Determines whether filtering has been enabled for any resource.
     *
     * @param resources The set of resources to check for filtering, may be <code>null</code>.
     * @return <code>true</code> if at least one resource uses filtering, <code>false</code> otherwise.
     */
    private boolean isFilteringEnabled(Collection resources) {
        if (resources != null) {
            for (Iterator i = resources.iterator(); i.hasNext(); ) {
                Resource resource = (Resource) i.next();
                if (resource.isFiltering()) {
                    return true;
                }
            }
        }
        return false;
    }


    public File getOutputDirectory() {
        return outputDirectory;
    }

    public List getResources() {

        return resources;
    }


}
