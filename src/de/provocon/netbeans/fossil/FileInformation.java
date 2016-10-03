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


/**
 * This file contains example file status, you may change/add/remove them as you wish to suit your versioning system.
 *
 * @author Maros Sandor
 */
public class FileInformation {

    /**
     * There is nothing known about the file, it may not even exist.
     */
    public static final int STATUS_UNKNOWN = 0;

    /**
     * The file is not managed by the module, i.e. the user does not wish it to be under control of this
     * versioning system module. All files except files under versioned roots have this status.
     */
    public static final int STATUS_NOTVERSIONED_NOTMANAGED = 1;

    /**
     * The file exists locally but is NOT under version control because it should not be (i.e. is has
     * the Ignore property set or resides under an excluded folder). The file itself IS under a versioned root.
     */
    public static final int STATUS_NOTVERSIONED_EXCLUDED = 2;

    /**
     * The file exists locally but is NOT under version control, mostly because it has not been added
     * to the repository yet.
     */
    public static final int STATUS_NOTVERSIONED_NEWLOCALLY = 4;

    /**
     * The file is under version control and is in sync with repository.
     */
    public static final int STATUS_VERSIONED_UPTODATE = 8;

    /**
     * The file is modified locally and was not yet modified in repository.
     */
    public static final int STATUS_VERSIONED_MODIFIEDLOCALLY = 16;

    /**
     * Status constant.
     */
    private final int status;

    /**
     * Directory indicator.
     */
    private final boolean directory;


    FileInformation(int status, boolean isDirectory) {
        this.status = status;
        this.directory = isDirectory;
    }


    public int getStatus() {
        return status;
    }


    public boolean isDirectory() {
        return directory;
    }

}
