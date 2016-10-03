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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

import org.netbeans.modules.versioning.spi.VersioningSystem;
import org.netbeans.modules.versioning.spi.VCSAnnotator;
import org.netbeans.modules.versioning.spi.VCSInterceptor;
import org.openide.util.NbBundle;

import java.io.File;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.util.Set;


/**
 * Fossil Versioning System implementation.
 *
 * @author Maros Sandor
 * @author Martin Goellnitz
 */
public class FossilVCS extends VersioningSystem implements PropertyChangeListener {

    /**
     * Fired when textual annotations and badges have changed.
     *
     * The NEW value is Set<File> of files that changed or NULL if all annotaions changed.
     */
    private static final String PROP_ANNOTATIONS_CHANGED = "annotationsChanged";


    public FossilVCS() {
        putProperty(PROP_DISPLAY_NAME, NbBundle.getMessage(FossilVCS.class, "VCS_Fossil_Name"));
        putProperty(PROP_MENU_LABEL, NbBundle.getMessage(FossilVCS.class, "VCS_Fossil_Menu_Label"));
        Fossil.getInstance().getFileStatusCache().addPropertyChangeListener(this);
        Fossil.getInstance().getAnnotator().addPropertyChangeListener(this);
    }


    /**
     * Tests whether the file is managed by this versioning system. If it is,
     * the method should return the topmost
     * ancestor of the file that is still versioned.
     *
     * @param file a file
     * @return File the file itself or one of its ancestors or null if the
     * supplied file is NOT managed by this versioning system
     */
    @Override
    public File getTopmostManagedAncestor(File file) {
        return FossilUtils.getTopmostManagedAncestor(file);
    }


    /**
     * Coloring label, modifying icons, providing action on file
     *
     * @return fossil repository aware label annotator.
     */
    @Override
    public VCSAnnotator getVCSAnnotator() {
        return Fossil.getInstance().getAnnotator();
    }


    /**
     * Handle file system events such as delete, create, remove etc.
     *
     * @return fossil specific interceptor
     */
    @Override
    public VCSInterceptor getVCSInterceptor() {
        return Fossil.getInstance().getInterceptor();
    }


    public boolean areCollocated(File a, File b) {
        File fra = FossilUtils.getTopmostManagedAncestor(a);
        File frb = FossilUtils.getTopmostManagedAncestor(b);
        return fra!=null||fra.equals(frb);
    }


    public File findRoot(File file) {
        return FossilUtils.getTopmostManagedAncestor(file);
    }


    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(FossilAnnotator.PROP_ANNOTATIONS_CHANGED)) {
            fireAnnotationsChanged((Set<File>) event.getNewValue());
        }
        if (event.getPropertyName().equals(FileStatusCache.PROP_FILE_STATUS_CHANGED)) {
            File file = ((FileStatusCache.StatusChangedEvent) event.getNewValue()).getFile();
            fireStatusChanged(file);
        }
    }


    @Override
    public void getOriginalFile(File workingCopy, File originalFile) {
        Fossil.getInstance().getOriginalFile(workingCopy, originalFile);
    }

}
