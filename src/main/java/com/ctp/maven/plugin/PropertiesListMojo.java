package com.ctp.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Goal which touches a timestamp file.
 *
 * @goal writeProperties
 * @phase process-sources
 */
public class PropertiesListMojo extends AbstractMojo {

    private static final String FILENAME = "properties.html";
    /**
     * Location of the file.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    public void execute()
            throws MojoExecutionException {
        File f = outputDirectory;

        if (!f.exists()) {
            f.mkdirs();
        }

        File file = new File(f, FILENAME);

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(file);

            /*
            Map pluginContext = this.getPluginContext();
            for (Object key: pluginContext.keySet()) {
                Object val = pluginContext.get(key);
                System.out.println(key.toString() + " (" + val.getClass().getCanonicalName() + ") = " + val.toString());
            }
            */
            for (Object filter : getFilters()) {
                Properties p = new Properties();
                System.out.println("FILE: " + filter.toString());
                p.load(new FileInputStream(filter.toString()));
                for(Object key : p.keySet()) {
                    System.out.println("> " + key + " = " + p.get(key));
                }
            }

            //pw.write("touch.txt");
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating file " + file, e);
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    private MavenProject getMavenProject() {
        return (MavenProject) this.getPluginContext().get("project");
    }

    private List getFilters() {
        return getMavenProject().getFilters();
    }
}
