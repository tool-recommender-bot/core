// -----BEGIN DISCLAIMER-----
/*******************************************************************************
 * Copyright (c) 2019 JCrypTool Team and Contributors
 * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
// -----END DISCLAIMER-----
package org.jcryptool.crypto.keystore.ui.dialogs.contentproviders.keypair;

import java.util.ArrayList;
import java.util.List;

import org.jcryptool.crypto.keystore.ui.dialogs.TableEntry;
import org.jcryptool.crypto.keystore.ui.dialogs.contentproviders.CertificateContentProvider;
import org.jcryptool.crypto.keystore.ui.dialogs.contentproviders.Messages;

import de.flexiprovider.api.exceptions.InvalidKeySpecException;
import de.flexiprovider.api.keys.Key;
import de.flexiprovider.nf.iq.iqdsa.IQDSAKeyFactory;
import de.flexiprovider.nf.iq.iqdsa.IQDSAParameterSpec;
import de.flexiprovider.nf.iq.iqdsa.IQDSAPublicKey;
import de.flexiprovider.nf.iq.iqdsa.IQDSAPublicKeySpec;

/**
 * @author Anatoli Barski
 * 
 */
public class IQDSAPublicKeyContentProvider extends CertificateContentProvider {

    @Override
    protected List<TableEntry> getAlgorithmElements(Object inputElement) {

        List<TableEntry> paramElements = new ArrayList<TableEntry>();

        try {
            IQDSAPublicKey key = (IQDSAPublicKey) inputElement;
            if (key == null)
                return null;

        } catch (ClassCastException e) {
            return null;
        }
        return paramElements;
    }

    @Override
    protected List<TableEntry> getKeySpecElements(Key key) {

        List<TableEntry> paramElements = new ArrayList<TableEntry>();

        try {
            IQDSAKeyFactory keyFactory = new IQDSAKeyFactory();
            IQDSAPublicKeySpec keySpec = (IQDSAPublicKeySpec) keyFactory.getKeySpec(key, IQDSAPublicKeySpec.class);
            if (keySpec == null)
                return null;
            paramElements.add(new TableEntry(Messages.ContentProvider_alpha, "" + keySpec.getAlpha())); //$NON-NLS-2$

            paramElements.addAll(getParameters(keySpec));
        } catch (ClassCastException e) {
            return null;
        } catch (InvalidKeySpecException e) {
            return null;
        }
        return paramElements;
    }

    private List<TableEntry> getParameters(IQDSAPublicKeySpec keySpec) {
        IQDSAParameterSpec params = (IQDSAParameterSpec) keySpec.getParams();
        List<TableEntry> paramElements = new ArrayList<TableEntry>();
        paramElements.add(new TableEntry(Messages.ContentProvider_discriminant, "" + params.getDiscriminant())); //$NON-NLS-2$
        paramElements.add(new TableEntry(Messages.ContentProvider_gamma, "" + params.getGamma())); //$NON-NLS-2$
        return paramElements;
    }
}
