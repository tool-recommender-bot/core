// -----BEGIN DISCLAIMER-----
/*******************************************************************************
 * Copyright (c) 2019 JCrypTool Team and Contributors
 * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
// -----END DISCLAIMER-----
package org.jcryptool.crypto.keystore.ui.views.nodes.containers;

import org.jcryptool.crypto.keystore.keys.IKeyStoreAlias;
import org.jcryptool.crypto.keystore.ui.views.nodes.TreeNode;

public abstract class AbstractContainerNode extends TreeNode {

    public AbstractContainerNode(String name) {
        super(name);
    }

    public abstract void add(IKeyStoreAlias alias);

    public abstract void remove(IKeyStoreAlias alias);

}
