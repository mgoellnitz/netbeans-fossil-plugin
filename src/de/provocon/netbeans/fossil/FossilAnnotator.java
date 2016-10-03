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

import org.netbeans.modules.versioning.spi.VCSAnnotator;
import org.netbeans.modules.versioning.spi.VCSContext;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;


/**
 * Annotator is resposible for file labels (coloring, badges) and for providing Versioning menu actions.
 *
 * @author Maros Sandor
 * @author Martin Goellnitz
 */
public class FossilAnnotator extends VCSAnnotator {

    private static final Logger LOG = Logger.getLogger("de.provocon.netbeans.fossil");

    /**
     * Fired when textual annotations and badges have changed. The NEW value is Set<File> of files that changed or NULL
     * if all annotations changed.
     */
    public static final String PROP_ANNOTATIONS_CHANGED = "annotationsChanged";

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);


    private String htmlEncode(String name) {
        if (name!=null&&name.length()>0) {
            name = name.replace("<", "&lt;");
            name = name.replace(">", "&gt;");
        }
        return name;
    }


    public String annotateName(String name, VCSContext context) {
        String annotatedName = super.annotateName(htmlEncode(name), context);
        File[] modifiedFiles = Fossil.getInstance().getFileStatusCache().listFiles(context, FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY);
        boolean modified = false;
        for (File f : context.getFiles()) {
            LOG.log(Level.FINE, "annotateName() c: {0}", f);
        }
        for (File f : modifiedFiles) {
            modified = modified||context.contains(f);
            LOG.log(Level.FINE, "annotateName() f: {0}", f);
        }
        if (modified) {
            annotatedName = "<font color=\"#6000FF\">"+htmlEncode(name)+" *</span>";
        } else {
            if (context.getFiles().size()==1&&context.getFiles().iterator().next().isFile()) {
                File[] unversionedFiles = Fossil.getInstance().getFileStatusCache().listFiles(context, FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY);
                boolean unversioned = false;
                for (File f : unversionedFiles) {
                    unversioned = unversioned||context.contains(f);
                    LOG.log(Level.FINE, "f: {0}", f);
                }
                if (unversioned) {
                    annotatedName = "<font color=\"#00C000\">"+htmlEncode(name)+"+ </span>";
                }
            }
        }
        LOG.log(Level.INFO, "annotateName() name: {0} -> {1} ({2})", new Object[]{name, annotatedName, context.getFiles().size()});
        return annotatedName;
    }


    public Image annotateIcon(Image icon, VCSContext context) {
        // TODO: examine content of the VCSContext and modify the icon if needed
        return super.annotateIcon(icon, context);
    }


    public Action[] getActions(VCSContext context, ActionDestination destination) {
        LOG.log(Level.INFO, "getActions() context={0} destination={1}", new Object[]{context, destination});
        List<Action> actions = new ArrayList<>();
        if (destination!=ActionDestination.MainMenu) {
            if (CommitAction.canCommit(context)) {
                actions.add(new CommitAction(context));
            }
            if (RevertAction.canRevert(context)) {
                actions.add(new RevertAction(context));
            }
            if (AddAction.canAdd(context)) {
                actions.add(new AddAction(context));
            }
        }
        return actions.toArray(new Action[actions.size()]);
    }


    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }


    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

}
