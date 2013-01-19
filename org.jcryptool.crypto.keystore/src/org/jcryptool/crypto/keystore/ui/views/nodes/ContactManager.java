// -----BEGIN DISCLAIMER-----
/*******************************************************************************
 * Copyright (c) 2008 JCrypTool Team and Contributors
 * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
// -----END DISCLAIMER-----
package org.jcryptool.crypto.keystore.ui.views.nodes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.jcryptool.core.logging.utils.LogUtil;
import org.jcryptool.crypto.keys.KeyType;
import org.jcryptool.crypto.keystore.KeyStorePlugin;
import org.jcryptool.crypto.keystore.backend.KeyStoreAlias;
import org.jcryptool.crypto.keystore.backend.KeyStoreManager;
import org.jcryptool.crypto.keystore.descriptors.interfaces.IContactDescriptor;
import org.jcryptool.crypto.keystore.ui.views.interfaces.IKeyStoreListener;

public class ContactManager {
    /** Singleton instance */
    private static ContactManager instance;

    private static ContactStore contactStore;

    /** All contacts and their respective meta information */
    private Map<String, IContactDescriptor> contactsDesc = Collections
            .synchronizedMap(new HashMap<String, IContactDescriptor>());

    private static List<IKeyStoreListener> listeners = new ArrayList<IKeyStoreListener>();

    private ITreeNode invisibleRoot;

    private static String DEFAULT_CONTACTS_XML;
    private static String USER_CONTACTS_XML;

    private ContactManager() {

        try {
            USER_CONTACTS_XML = Platform.getInstanceLocation().getURL().getPath() + "contacts.xml"; //$NON-NLS-1$
            IFileStore userContacts = EFS.getStore(new URI("file://" + USER_CONTACTS_XML)); //$NON-NLS-1$
            if (!userContacts.fetchInfo().exists()) {
                URL url = FileLocator.find(Platform.getBundle(KeyStorePlugin.PLUGIN_ID), new Path(
                        "contactstore/contacts.xml"), null); //$NON-NLS-1$
                try {
                    DEFAULT_CONTACTS_XML = FileLocator.toFileURL(url).getPath();
                } catch (IOException e) {
                    LogUtil.logError(KeyStorePlugin.PLUGIN_ID, Messages.ContactManager_0, e, true);
                }

                IFileStore defaultContacts = EFS.getStore(new URI("file://" + DEFAULT_CONTACTS_XML)); //$NON-NLS-1$

                defaultContacts.copy(userContacts, 0, null);
            }
        } catch (Exception e) {
            LogUtil.logError(KeyStorePlugin.PLUGIN_ID, Messages.ContactManager_5, e, true);
        }

    }

    public synchronized static ContactManager getInstance() {
        if (instance == null) {
            instance = new ContactManager();
            instance.initTreeModel();
        }
        return instance;
    }

    private void initTreeModel() {
        if (invisibleRoot == null) {
            init();
            invisibleRoot = new TreeNode("INVISIBLE_ROOT"); //$NON-NLS-1$
            for (IContactDescriptor desc : contactsDesc.values()) {
                if (desc instanceof ContactDescriptorNode) {
                    LogUtil.logInfo("adding: " + desc.getContact().getName()); //$NON-NLS-1$
                    invisibleRoot.addChild((ContactDescriptorNode) desc);
                }
            }
            LogUtil.logInfo("children.length: " + invisibleRoot.getChildrenArray().length); //$NON-NLS-1$
        }
    }

    public void addKeyStoreListener(IKeyStoreListener listener) {
        listeners.add(listener);
    }

    public void removeKeyStoreListener(IKeyStoreListener listener) {
        listeners.remove(listener);
    }

    public Iterator<IKeyStoreListener> getKeyStoreListeners() {
        return listeners.iterator();
    }

    public ITreeNode getTreeModel() {
        return invisibleRoot;
    }

    private void init() {
        try {
            contactsDesc = loadContacts(getContactStore());
        } catch (Exception ex) {
            LogUtil.logError(KeyStorePlugin.PLUGIN_ID, Messages.ContactManager_6, ex, true);
            return;
        }

        Enumeration<String> aliases = null;
        try {
            aliases = KeyStoreManager.getInstance().getAliases();
        } catch (KeyStoreException e) {
            LogUtil.logError(KeyStorePlugin.PLUGIN_ID, "KeyStoreException while accessing the aliases", e, true); //$NON-NLS-1$
        }
        Map<String, List<KeyStoreAlias>> aliasesHashed = new HashMap<String, List<KeyStoreAlias>>();
        while (aliases.hasMoreElements()) {
            KeyStoreAlias alias = new KeyStoreAlias(aliases.nextElement());
            if (!aliasesHashed.containsKey(alias.getContactName()))
                aliasesHashed.put(alias.getContactName(), new ArrayList<KeyStoreAlias>());
            aliasesHashed.get(alias.getContactName()).add(alias);
        }

        for (Entry<String, IContactDescriptor> entry : contactsDesc.entrySet()) {
            if (aliasesHashed.containsKey(entry.getKey())) {
                for (KeyStoreAlias alias : aliasesHashed.get(entry.getKey()))
                    addEntryToContact(entry.getValue(), alias);
            }
        }

        notifyListeners();
    }

    private Map<String, IContactDescriptor> loadContacts(ContactStore cStore) throws FileNotFoundException,
            JAXBException {
        Map<String, IContactDescriptor> contacts = Collections
                .synchronizedMap(new HashMap<String, IContactDescriptor>());
        for (Contact contact : getContactStore().getContacts())
            contacts.put(contact.getName(), new ContactDescriptorNode(contact));
        return contacts;
    }

    private void storeContacts(ContactStore cStore) throws JAXBException {
        cStore.write(USER_CONTACTS_XML);
    }

    private ContactStore getContactStore() throws FileNotFoundException, JAXBException {
        if (contactStore == null)
            contactStore = ContactStore.read(USER_CONTACTS_XML);
        return contactStore;
    }

    private void addEntryToContact(IContactDescriptor contact, KeyStoreAlias alias) {
        LogUtil.logInfo("Adding entry to contact " + alias.getAliasString()); //$NON-NLS-1$

        if (alias.getKeyStoreEntryType().equals(KeyType.SECRETKEY)) {
            contact.addSecretKey(alias);
        } else if (alias.getKeyStoreEntryType().equals(KeyType.PUBLICKEY)) {
            contact.addCertificate(alias);
        } else if (alias.getKeyStoreEntryType().equals(KeyType.KEYPAIR_PRIVATE_KEY)) {
            contact.addKeyPair(alias, null);
        } else if (alias.getKeyStoreEntryType().equals(KeyType.KEYPAIR_PUBLIC_KEY)) {
            contact.addKeyPair(null, alias);
        }
    }

    public boolean contactExists(String name) {
        return contactsDesc.containsKey(name);
    }

    public IContactDescriptor newContact(String name) {
        Contact contact = new Contact();
        contact.setName(name);
        return newContact(contact);
    }

    public IContactDescriptor newContact(Contact contact) {
        IContactDescriptor newContactDesc = new ContactDescriptorNode(contact);
        addContact(newContactDesc);
        return newContactDesc;
    }

    private void addContact(IContactDescriptor contactDesc) {
        if (contactExists(contactDesc.getContact().getName())) {
            LogUtil.logInfo("Contact already exists"); //$NON-NLS-1$
            return;
        }
        try {
            getContactStore().getContacts().add(contactDesc.getContact());
            storeContacts(getContactStore());
        } catch (Exception e) {
            LogUtil.logError(KeyStorePlugin.PLUGIN_ID, Messages.ContactManager_7, e, true);
            return;
        }
        invisibleRoot.addChild((ContactDescriptorNode) contactDesc);
        contactsDesc.put(contactDesc.getContact().getName(), contactDesc);
        notifyListeners();
    }

    public void removeContact(String contactName) {
        LogUtil.logInfo("Removing contact " + contactName); //$NON-NLS-1$

        try {
            for (Contact c : getContactStore().getContacts()) {
                if (c.getName().equals(contactName)) {
                    getContactStore().getContacts().remove(c);
                    break;
                }
            }
            storeContacts(getContactStore());
        } catch (Exception e) {
            LogUtil.logError(KeyStorePlugin.PLUGIN_ID, Messages.ContactManager_8, e, true);
            return;
        }

        invisibleRoot.removeChild((ContactDescriptorNode) contactsDesc.get(contactName));
        contactsDesc.remove(contactName);
        notifyListeners();
    }

    private void notifyListeners() {
        Iterator<IKeyStoreListener> it = getKeyStoreListeners();
        while (it.hasNext()) {
            it.next().fireKeyStoreModified(invisibleRoot);
        }
    }

    public void removeEntry(KeyStoreAlias alias) {
        LogUtil.logInfo("Removing entry " + alias.getAliasString()); //$NON-NLS-1$

        if (alias.getKeyStoreEntryType().equals(KeyType.SECRETKEY)) {
            LogUtil.logInfo("removing a secret key"); //$NON-NLS-1$
            contactsDesc.get(alias.getContactName()).removeSecretKey(alias);
        } else if (alias.getKeyStoreEntryType().equals(KeyType.PUBLICKEY)) {
            LogUtil.logInfo("removing a certificate"); //$NON-NLS-1$
            contactsDesc.get(alias.getContactName()).removeCertificate(alias);
        } else if (alias.getKeyStoreEntryType().equals(KeyType.KEYPAIR_PRIVATE_KEY)) {
            LogUtil.logInfo("removing a key pair"); //$NON-NLS-1$
            contactsDesc.get(alias.getContactName()).removeKeyPair(alias);
        } else if (alias.getKeyStoreEntryType().equals(KeyType.KEYPAIR_PUBLIC_KEY)) {
            LogUtil.logInfo("removing a key pair"); //$NON-NLS-1$
            contactsDesc.get(alias.getContactName()).removeKeyPair(alias);
        }

        notifyListeners();
    }

    public void addCertificate(KeyStoreAlias alias) {
        LogUtil.logInfo("Adding certificate " + alias.getAliasString()); //$NON-NLS-1$

        if (contactExists(alias.getContactName())) {
            contactsDesc.get(alias.getContactName()).addCertificate(alias);
        } else {
            IContactDescriptor contact = newContact(new Contact(alias.getContactName(), null, null, null, null));
            contact.addCertificate(alias);
        }

        notifyListeners();
    }

    public void addKeyPair(KeyStoreAlias privateKey, KeyStoreAlias publicKey) {
        LogUtil.logInfo("Adding key pair " + publicKey.getAliasString()); //$NON-NLS-1$

        if (contactExists(privateKey.getContactName())) {
            contactsDesc.get(privateKey.getContactName()).addKeyPair(privateKey, publicKey);
        } else {
            IContactDescriptor contact = newContact(new Contact(privateKey.getContactName(), null, null, null, null));
            contact.addKeyPair(privateKey, publicKey);
        }

        notifyListeners();
    }

    public void addSecretKey(KeyStoreAlias alias) {
        LogUtil.logInfo("Adding secret key " + alias.getAliasString()); //$NON-NLS-1$

        if (contactExists(alias.getContactName())) {
            contactsDesc.get(alias.getContactName()).addSecretKey(alias);
        } else {
            IContactDescriptor contact = newContact(new Contact(alias.getContactName(), null, null, null, null));
            contact.addSecretKey(alias);
        }

        notifyListeners();
    }

    public Iterator<Contact> getContacts() {
        try {
            return getContactStore().getContacts().iterator();
        } catch (Exception e) {
            LogUtil.logError(KeyStorePlugin.PLUGIN_ID, Messages.ContactManager_9, e, true);
        }

        return new ArrayList<Contact>().iterator();
    }

    public int getContactSize() {
        LogUtil.logInfo("Contacts size is " + contactsDesc.size()); //$NON-NLS-1$
        return contactsDesc.size();
    }
}
