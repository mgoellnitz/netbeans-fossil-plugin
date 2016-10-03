/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package de.provocon.netbeans.fossil;

import java.io.BufferedReader;
import org.netbeans.modules.versioning.spi.VCSInterceptor;
import org.netbeans.modules.versioning.spi.VersioningSupport;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Fossil Versioning system implementation.
 *
 * @author Martin Goellnitz
 */
public class Fossil {

    private static final Logger LOG = Logger.getLogger("de.provocon.netbeans.fossil");

    private static Fossil instance;

    private VCSInterceptor interceptor;

    private FileStatusCache fileStatusCache;

    private FossilAnnotator annotator;


    public static Fossil getInstance() {
        synchronized (Fossil.class) {
            if (instance==null) {
                instance = new Fossil();
                instance.init();
            }
            return instance;
        }
    }


    private void init() {
        fileStatusCache = new FileStatusCache();
        annotator = new FossilAnnotator();
        interceptor = new FossilInterceptor();
    }


    public FileStatusCache getFileStatusCache() {
        return fileStatusCache;
    }


    public FossilAnnotator getAnnotator() {
        return annotator;
    }


    /**
     * Tests whether a file or directory should receive the STATUS_NOTVERSIONED_NOTMANAGED status.
     *
     * @param file a file or directory
     * @return false if the file should receive the STATUS_NOTVERSIONED_NOTMANAGED status, true otherwise
     */
    public boolean isManaged(File file) {
        String path;
        try {
            path = file.getCanonicalPath();
        } catch (Exception e) {
            path = e.getMessage();
        }
        boolean result = VersioningSupport.getOwner(file) instanceof FossilVCS;
        LOG.log(Level.SEVERE, "isManaged() {0}: {1}", new Object[]{path, result});
        return result;
    }


    public VCSInterceptor getInterceptor() {
        LOG.warning("getInterceptor()");
        return interceptor;
    }


    public void getOriginalFile(File workingCopy, File originalFile) {
        try {
            String path = workingCopy.getCanonicalPath();
            String parentPath = originalFile.getParentFile().getCanonicalPath();

            boolean mkdirs = originalFile.getParentFile().mkdirs();
            LOG.log(Level.INFO, "getOriginalFile() created folders {0}: {1} {2}", new Object[]{parentPath, originalFile.getParentFile().exists(), mkdirs});
            LOG.log(Level.INFO, "getOriginalFile() {0} to {1}", new Object[]{path, originalFile.getCanonicalPath()});
            String cmd[] = new String[3];
            cmd[0] = "fossil";
            cmd[1] = "cat";
            cmd[2] = path;
            Process process = Runtime.getRuntime().exec(cmd, null, FossilUtils.getTopmostManagedAncestor(workingCopy));
            InputStream stdout = process.getInputStream();
            InputStream stderr = process.getErrorStream();
            process.waitFor(2, TimeUnit.SECONDS);
            LOG.log(Level.INFO, "getOriginalFile() reading errors");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stderr));
            String line = reader.readLine();
            while (line!=null) {
                LOG.log(Level.SEVERE, "getOriginalFile() {0}", new Object[]{line});
                line = reader.readLine();
            }
            LOG.log(Level.INFO, "getOriginalFile() reading contents");
            FileWriter writer = new FileWriter(originalFile);
            reader = new BufferedReader(new InputStreamReader(stdout));
            line = reader.readLine();
            while (line!=null) {
                LOG.log(Level.FINE, "getOriginalFile() {0}", new Object[]{line});
                writer.write(line);
                writer.write("\n");
                writer.flush();
                line = reader.readLine();
            }
            LOG.log(Level.FINE, "getOriginalFile() exit value is {0}", new Object[]{process.exitValue()});
            writer.close();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "getOriginalFile() failed: {0}", new Object[]{ex.getMessage()});
        }
    }

}
