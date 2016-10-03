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

import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.spi.VersioningSupport;
import org.openide.util.RequestProcessor;

import java.io.File;
import java.io.IOException;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Example status cache that holds versioning status information for files and folders.
 *
 * @author Maros Sandor
 * @author Martin Goellnitz
 */
public class FileStatusCache {

    private static final Logger LOG = Logger.getLogger("de.provocon.netbeans.fossil");

    public static final String PROP_FILE_STATUS_CHANGED = "fileStatusChanged";

    private static final FileInformation FILE_INFORMATION_NOTMANAGED = new FileInformation(FileInformation.STATUS_NOTVERSIONED_NOTMANAGED, false);

    private static final FileInformation FILE_INFORMATION_NOTMANAGED_DIRECTORY = new FileInformation(FileInformation.STATUS_NOTVERSIONED_NOTMANAGED, true);

    public static final FileInformation FILE_INFORMATION_UNKNOWN = new FileInformation(FileInformation.STATUS_UNKNOWN, false);

    private final Map<File, Map<File, FileInformation>> statusMap = new HashMap<>();

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private final Set<File> filesToRefresh = new HashSet<>();

    private RequestProcessor.Task filesToRefreshTask;

    private RequestProcessor rp;


    /**
     * Retrieves list of files in the given context having the desired status.
     *
     * @param context context to examine
     * @param includeStatus desired status
     */
    public File[] listFiles(VCSContext context, int includeStatus) {
        Set<File> set = new HashSet<>();

        for (Map.Entry<File, Map<File, FileInformation>> entry : statusMap.entrySet()) {
            LOG.log(Level.INFO, "entry: {0} {1}", new Object[]{includeStatus, entry.getKey()});
            Map<File, FileInformation> map = entry.getValue();
            for (Iterator<File> i = map.keySet().iterator(); i.hasNext();) {
                File file = i.next();
                FileInformation info = map.get(file);
                if ((info.getStatus()&includeStatus)==0) {
                    continue;
                }

                if (context!=null) {
                    for (File root : context.getRootFiles()) {
                        if (VersioningSupport.isFlat(root)) {
                            if (file.equals(root)||file.getParentFile().equals(root)) {
                                set.add(file);
                                break;
                            }
                        } else {
                            if (FossilUtils.isAncestorOrEqual(root, file)) {
                                set.add(file);
                                break;
                            }
                        }
                    }
                } else {
                    set.add(file);
                }
            }
        }

        if (context!=null&&context.getExclusions().size()>0) {
            for (Iterator<File> i = context.getExclusions().iterator(); i.hasNext();) {
                File excluded = i.next();
                for (Iterator<File> j = set.iterator(); j.hasNext();) {
                    File file = j.next();
                    if (FossilUtils.isAncestorOrEqual(excluded, file)) {
                        j.remove();
                    }
                }
            }
        }
        return set.toArray(new File[set.size()]);
    }


    private void put(File dir, Map<File, FileInformation> newDirMap) {
        LOG.log(Level.INFO, "put()  {0}", dir);
        synchronized (this) {
            statusMap.put(dir, newDirMap);
        }
    }


    private Map<File, FileInformation> get(File dir) {
        LOG.log(Level.INFO, "get() {0}", dir);
        synchronized (this) {
            return statusMap.get(dir);
        }
    }


    /**
     * Returns the versionig status for a file as long it is already stored in the cache or {@link #FILE_INFORMATION_UNKNOWN}.
     * If refreshUnknown true and no status value is available at the moment a status refresh for the given file is triggered
     * asynchronously and subsequently status change events will be fired to notify all registered listeners.
     *
     * @param file file to get status for
     * @return FileInformation structure containing the file status or {@link #FILE_INFORMATION_UNKNOWN} if there is no staus known yet
     * @see FileInformation
     */
    public FileInformation getCachedInfo(final File file) {
        File dir = file.getParentFile();

        if (dir==null) {
            return FILE_INFORMATION_NOTMANAGED; // default for filesystem roots
        }

        Map<File, FileInformation> dirMap = get(dir);
        FileInformation info = dirMap!=null ? dirMap.get(file) : null;
        if (info==null) {
            if (!Fossil.getInstance().isManaged(file)) {
                return file.isDirectory() ? FILE_INFORMATION_NOTMANAGED_DIRECTORY : FILE_INFORMATION_NOTMANAGED;
            }
        }
        return info;
    }


    /**
     * Determines the versioning status information for a file.
     * This method synchronously accesses disk and may block for a long period of time.
     *
     * @param file file to get the {@link FileInformation} for
     * @return FileInformation structure containing the file status
     * @see FileInformation
     */
    public FileInformation getInfo(File file) {
        FileInformation fi = getCachedInfo(file);
        if (fi==null) {
            fi = refresh(file, false);
        }
        return fi;
    }


    /**
     * Asynchronously refreshes the status for the given files.
     * Status change events will be fired to notify all registered listeners.
     *
     * @param files
     */
    public void refreshLater(File... files) {
        synchronized (filesToRefresh) {
            for (File file : files) {
                if (file==null) {
                    continue;
                }
                filesToRefresh.add(file);
            }
        }
        getFilesToRefreshTask().schedule(200);
    }


    private RequestProcessor.Task getFilesToRefreshTask() {
        if (filesToRefreshTask==null) {
            filesToRefreshTask = getRequestProcessor().create(new Runnable() {
                public void run() {
                    File[] files;
                    synchronized (filesToRefresh) {
                        files = filesToRefresh.toArray(new File[filesToRefresh.size()]);
                        filesToRefresh.clear();
                    }
                    for (File file : files) {
                        refresh(file, false);
                    }
                }
            });
        }
        return filesToRefreshTask;
    }


    private RequestProcessor getRequestProcessor() {
        if (rp==null) {
            rp = new RequestProcessor("Fossil - FileStatusCache");
        }
        return rp;
    }


    /**
     * Refreshes the status value for the given file, all its siblings and its parent folder.
     * Status change events will be eventually thrown for the file, all its siblings and its parent folder.
     *
     * @param file the file to be refreshed
     * @param forceChangeEvent if true status change event will fired even if
     * the newly retrieved status value for a file is the same as the already cached one
     * @return FileInformation
     */
    private FileInformation refresh(File file, boolean forceChangeEvent) {
        // check if it is a managed directory structure
        File dir = file.getParentFile();
        if (dir==null) {
            return FileStatusCache.FILE_INFORMATION_NOTMANAGED; // default for filesystem roots
        }

        String path;
        try {
            path = file.getCanonicalPath();
        } catch (Exception e) {
            path = e.getMessage();
        }
        LOG.log(Level.SEVERE, "refresh() {0}", path);

        if (!Fossil.getInstance().isManaged(file)) {
            return file.isDirectory() ? FILE_INFORMATION_NOTMANAGED_DIRECTORY : FILE_INFORMATION_NOTMANAGED;
        }
        LOG.log(Level.SEVERE, "refresh() {0} is managed.", path);

        Map<File, FileInformation> oldDirMap = get(dir);
        Map<File, FileInformation> newDirMap = new HashMap<>();

        for (File child : dir.listFiles()) {
            FileInformation fi;
            if (FossilUtils.checkModified(child)) {
                fi = new FileInformation(FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY, child.isDirectory());
            } else {
                if (FossilUtils.checkUnversioned(child)) {
                    fi = new FileInformation(FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY, child.isDirectory());
                } else {
                    fi = new FileInformation(FileInformation.STATUS_VERSIONED_UPTODATE, child.isDirectory());
                }
            }
            newDirMap.put(child, fi);
        }
        put(dir, newDirMap);

        fireStatusEvents(newDirMap, oldDirMap, forceChangeEvent);
        return newDirMap.get(file);
    }


    private void fireStatusEvents(Map<File, FileInformation> newDirMap, Map<File, FileInformation> oldDirMap, boolean force) {
        for (File file : newDirMap.keySet()) {
            FileInformation newInfo;
            FileInformation oldInfo;
            try {
                newInfo = newDirMap.get(file.getCanonicalFile());
                oldInfo = oldDirMap!=null ? oldDirMap.get(file.getCanonicalFile()) : null;
                fireFileStatusChanged(file, oldInfo, newInfo, force);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "first {0}", ex.getMessage());
            }
        }
        if (oldDirMap==null) {
            return;
        }
        for (File file : oldDirMap.keySet()) {
            FileInformation newInfo = newDirMap.get(file);
            if (newInfo==null) {
                FileInformation oldInfo;
                try {
                    oldInfo = oldDirMap.get(file.getCanonicalFile());
                    fireFileStatusChanged(file, oldInfo, newInfo, force);
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, "second {0}", ex.getMessage());
                }
            }
        }
    }


    private void fireFileStatusChanged(File file, FileInformation oldInfo, FileInformation newInfo, boolean force) {
        force = false;
        if (!force) {
            if (oldInfo==null&&newInfo==null) {
                return;
            }
            if (oldInfo!=null&&newInfo!=null&&oldInfo.getStatus()==newInfo.getStatus()) {
                return;
            }
        }
        support.firePropertyChange(PROP_FILE_STATUS_CHANGED, null, new StatusChangedEvent(file, oldInfo, newInfo!=null ? newInfo : FILE_INFORMATION_UNKNOWN));
    }


    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }


    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }


    /**
     * Instances describe status changes.
     */
    public static class StatusChangedEvent {

        private final File file;

        private final FileInformation oldInfo;

        private final FileInformation newInfo;


        public StatusChangedEvent(File file, FileInformation oldInfo, FileInformation newInfo) {
            this.file = file;
            this.oldInfo = oldInfo;
            this.newInfo = newInfo;
        }


        public File getFile() {
            return file;
        }


        public FileInformation getOldInfo() {
            return oldInfo;
        }


        public FileInformation getNewInfo() {
            return newInfo;
        }
    }

}
