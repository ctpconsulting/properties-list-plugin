package com.ctp.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Goal which touches a timestamp file.
 *
 * @goal touch
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

            Map pluginContext = this.getPluginContext();
            for (Object key: pluginContext.keySet()) {
                Object val = pluginContext.get(key);
                System.out.println(key.toString() + " (" + val.getClass().getCanonicalName() + ") = " + val.toString());
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
}
