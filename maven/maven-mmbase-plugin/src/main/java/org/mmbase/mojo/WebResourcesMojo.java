/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

 */
package org.mmbase.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Mojo which copies the 'templates' and 'blocks' directory (if they exist) to the correct location
 * in the war. The whole exercise is only necessary, because you can't configure in pom that it
 * should not include a certain web-resource if the source happens to not exist.
 *
 * @phase process-resources
 * @goal install-resources
 * @requiresProject
 * @author Michiel Meeuwissen
 * @deprecated Simply move the jsp to the correct location in src/main/webapp.
 */

public class WebResourcesMojo extends AbstractMojo {

    private static final String[] DEFAULT_INCLUDES = { "**/**" };

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     *
     * @parameter expression=true
     */
    private boolean useDefaultExcludes = false;


    /**
     * The directory where the webapp is built.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     */
    private File webappDirectory;

    public void execute() throws MojoExecutionException {
        String packaging = project.getPackaging();
        if (packaging.equals("war")) {
            try {
                File templates = new File(project.getBasedir(), "templates");
                if (templates.isDirectory()) {
                    String templatesTarget = (String) project.getProperties().get("templatesTarget");
                    if (templatesTarget == null) {
                        templatesTarget = "mmbase/"
                            + project.getArtifactId().substring("mmbase-".length());
                    }
                    templatesTarget = templatesTarget.replace('/', File.separatorChar);
                    File target = new File(webappDirectory, templatesTarget);
                    copyDirectory(templates, target);
                }
                File blocks = new File(project.getBasedir(), "blocks");
                if (blocks.isDirectory()) {
                    String blocksTarget = (String) project.getProperties().get("blocksTarget");
                    if (blocksTarget == null) {
                        blocksTarget =
                            "mmbase" + File.separator + "components" + File.separator
                            + project.getArtifactId().substring("mmbase-".length());
                    }
                    blocksTarget = blocksTarget.replace('/', File.separatorChar);
                    File target = new File(webappDirectory, blocksTarget);
                    copyDirectory(blocks, target);
                }

                File mmbase = new File(project.getBasedir(), "mmbase");
                if (mmbase.isDirectory()) {
                    File target = new File(webappDirectory, "mmbase");
                    copyDirectory(mmbase, target);
                }
            } catch (IOException ioe) {
                throw new MojoExecutionException(ioe.getMessage(), ioe);
            }
        }
    }

    private void copyDirectory(File srcPath, File dstPath) throws IOException {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(srcPath);
        scanner.setIncludes(DEFAULT_INCLUDES);
        if (useDefaultExcludes) {
            scanner.addDefaultExcludes();
        }
        scanner.scan();

        List<String> includedFiles = Arrays.asList(scanner.getIncludedFiles());

        getLog().info("Copying " + +includedFiles.size() + " resource"
                      + (includedFiles.size() != 1 ? "s" : "")
                      + (dstPath == null ? "" : " to " + dstPath));

        for (Iterator<String> j = includedFiles.iterator(); j.hasNext();) {
            String name = j.next();

            File source = new File(srcPath, name);
            File destinationFile = new File(dstPath, name);
            if (!destinationFile.getParentFile().exists()) {
                destinationFile.getParentFile().mkdirs();
            }
            FileUtils.copyFile(source, destinationFile, null, null);
        }
    }

}
