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
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.netbeans.modules.versioning.spi.VersioningSupport;


/**
 * Misc utility methods, mostly copied from onm.versioning.util module.
 *
 * @author Maros Sandor
 * @author Martin Goellnitz
 * @author Muhammed Yururdurmaz
 */
public final class FossilUtils {

    private static final Logger LOG = Logger.getLogger("de.provocon.netbeans.fossil");
    private static final Pattern EDITED_PATTERN = Pattern.compile("^EDITED[\t ]+.*");
    private static final Pattern ADDED_PATTERN = Pattern.compile("^ADDED[\t ]+.*");
    private static final Pattern REVERT_PATTERN = Pattern.compile("^REVERT[\t ]+.*");
    private static final Pattern NEW_VERSION_PATTERN = Pattern.compile("^New_Version[\t ]+.*");


    private FossilUtils() {
        // Protect utility collection from instanciation.
    }


    /**
     * Tests for ancestor/child file relationsip.
     *
     * @param ancestor supposed ancestor of the file
     * @param file a file
     * @return true if ancestor is an ancestor folder of file OR both parameters are equal, false otherwise
     */
    public static boolean isAncestorOrEqual(File ancestor, File file) {
        if (VersioningSupport.isFlat(ancestor)) {
            return ancestor.equals(file)||ancestor.equals(file.getParentFile())&&!file.isDirectory();
        }
        for (; file!=null; file = file.getParentFile()) {
            if (file.equals(ancestor)) {
                return true;
            }
        }
        return false;
    }


    public static File getTopmostManagedAncestor(File file) {
        File topmostAncestor = null;
        String path;
        try {
            path = file.getCanonicalPath();
            if (file.isFile()) {
                file = file.getParentFile();
            }
            for (; file!=null; file = file.getParentFile()) {
                String[] fileList = file.list();
                for (String f : fileList) {
                    if (".fslckout".equals(f)) {
                        topmostAncestor = file;
                    }
                }
            }
        } catch (Exception e) {
            path = e.getMessage();
        }
        LOG.log(Level.FINE, "getTopmostManagedAncestor() {0}: {1}", new Object[]{path, topmostAncestor});
        return topmostAncestor;
    }


    public static boolean fossilCommandWithConstantCheck(String[] cmd, Pattern successMarker, File file) {
        boolean result = false;
        try {
            Process process = Runtime.getRuntime().exec(cmd, null, FossilUtils.getTopmostManagedAncestor(file));
            InputStream stdout = process.getInputStream();
            InputStream stderr = process.getErrorStream();
            process.waitFor(2, TimeUnit.SECONDS);
            LOG.log(Level.FINE, "fossilCommandWithConstantCheck() reading errors for {}", cmd[1]);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stderr));
            String line = reader.readLine();
            while (line!=null) {
                LOG.log(Level.SEVERE, "fossilCommandWithConstantCheck() {0} {1}", new Object[]{cmd[1], line});
                line = reader.readLine();
            }
            LOG.log(Level.FINE, "fossilCommandWithConstantCheck() reading contents");
            reader = new BufferedReader(new InputStreamReader(stdout));
            line = reader.readLine();
            while (line!=null && !result) {
                result = result||successMarker.matcher(line).matches();
                LOG.log(Level.FINE, "fossilCommandWithConstantCheck() {0} {1}", new Object[]{result, line});
                line = reader.readLine();
            }
            LOG.log(Level.FINE, "fossilCommandWithConstantCheck() exit value is {0}", new Object[]{process.exitValue()});
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "fossilCommandWithConstantCheck() failed: {0} {1}", new Object[]{ex.getClass().getSimpleName(), ex.getMessage()});
        }
        LOG.log(Level.INFO, "fossilCommandWithConstantCheck() {0} {1} {2}", new Object[]{cmd[1], file, result});
        return result;
    }


    public static boolean checkModified(File file) {
        boolean result = false;
        try {
            String path = file.getCanonicalPath();
            String cmd[] = new String[3];
            cmd[0] = "fossil";
            cmd[1] = "status";
            cmd[2] = path;

            result = fossilCommandWithConstantCheck(cmd, EDITED_PATTERN, file);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "checkModified() failed: {0} {1}", new Object[]{ex.getClass().getSimpleName(), ex.getMessage()});
        }
        LOG.log(Level.INFO, "checkModified() {0} {1}", new Object[]{file, result});
        return result;
    }


    public static boolean checkUnversioned(File file) {
        boolean result = false;
        try {
            String path = file.getCanonicalPath();
            String cmd[] = new String[3];
            cmd[0] = "fossil";
            cmd[1] = "extras";
            cmd[2] = path;

            Process process = Runtime.getRuntime().exec(cmd, null, FossilUtils.getTopmostManagedAncestor(file));
            InputStream stdout = process.getInputStream();
            InputStream stderr = process.getErrorStream();
            process.waitFor(2, TimeUnit.SECONDS);
            LOG.log(Level.FINE, "checkUnversioned() reading errors");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stderr));
            String line = reader.readLine();
            while (line!=null) {
                LOG.log(Level.SEVERE, "checkUnversioned() {0}", new Object[]{line});
                line = reader.readLine();
            }
            LOG.log(Level.FINE, "checkUnversioned() reading contents");
            reader = new BufferedReader(new InputStreamReader(stdout));
            line = reader.readLine();
            while (line!=null) {
                result = result||file.getPath().contains(line);
                LOG.log(Level.FINE, "checkUnversioned() {0} {1} {2}", new Object[]{result, file.getPath(), line});
                line = reader.readLine();
            }
            LOG.log(Level.FINE, "checkUnversioned() exit value is {0}", new Object[]{process.exitValue()});
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "checkUnversioned() failed: {0} {1}", new Object[]{ex.getClass().getSimpleName(), ex.getMessage()});
        }
        LOG.log(Level.INFO, "checkUnversioned() {0} {1}", new Object[]{file, result});
        return result;
    }


    public static boolean add(Set<File> files) {
        boolean result = false;
        try {
            String cmd[] = new String[files.size()+2];
            int i = 0;
            cmd[i++] = "fossil";
            cmd[i++] = "add";
            for (File f : files) {
                String path = f.getCanonicalPath();
                cmd[i++] = path;
            }

            result = fossilCommandWithConstantCheck(cmd, ADDED_PATTERN, FossilUtils.getTopmostManagedAncestor(files.iterator().next()));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "add() failed: {0} {1}", new Object[]{ex.getClass().getSimpleName(), ex.getMessage()});
        }
        LOG.log(Level.INFO, "add() {0}", result);
        return result;
    }


    public static boolean revert(Set<File> files) {
        boolean result = false;
        try {
            String cmd[] = new String[files.size()+2];
            int i = 0;
            cmd[i++] = "fossil";
            cmd[i++] = "revert";
            for (File f : files) {
                String path = f.getCanonicalPath();
                cmd[i++] = path;
            }

            result = fossilCommandWithConstantCheck(cmd, REVERT_PATTERN, FossilUtils.getTopmostManagedAncestor(files.iterator().next()));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "revert() failed: {0} {1}", new Object[]{ex.getClass().getSimpleName(), ex.getMessage()});
        }
        LOG.log(Level.INFO, "revert() {0}", result);
        return result;
    }


    public static boolean commit(Set<File> files, String msg) {
        boolean result = false;
        try {
            String cmd[] = new String[files.size()+4];
            int i = 0;
            cmd[i++] = "fossil";
            cmd[i++] = "commit";
            cmd[i++] = "-m";
            cmd[i++] = msg;
            for (File f : files) {
                String path = f.getCanonicalPath();
                cmd[i++] = path;
            }

            result = fossilCommandWithConstantCheck(cmd, NEW_VERSION_PATTERN, FossilUtils.getTopmostManagedAncestor(files.iterator().next()));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "commit() failed: {0} {1}", new Object[]{ex.getClass().getSimpleName(), ex.getMessage()});
        }
        LOG.log(Level.INFO, "commit() {0} {1}", new Object[]{msg, result});
        return result;
    }

}
