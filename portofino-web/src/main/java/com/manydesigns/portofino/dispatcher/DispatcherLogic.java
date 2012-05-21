/*
 * Copyright (C) 2005-2012 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.manydesigns.portofino.dispatcher;

import com.manydesigns.elements.options.DefaultSelectionProvider;
import com.manydesigns.elements.options.SelectionProvider;
import com.manydesigns.elements.util.ElementsFileUtils;
import com.manydesigns.portofino.PortofinoProperties;
import com.manydesigns.portofino.application.Application;
import com.manydesigns.portofino.pages.Page;
import com.manydesigns.portofino.scripting.ScriptingUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public class DispatcherLogic {
    public static final String copyright =
            "Copyright (c) 2005-2012, ManyDesigns srl";

    private static final Logger logger = LoggerFactory.getLogger(DispatcherLogic.class);

    public static SelectionProvider createPagesSelectionProvider
            (Application application, File baseDir, File... excludes) {
        return createPagesSelectionProvider(application, baseDir, false, false, excludes);
    }

    public static SelectionProvider createPagesSelectionProvider
            (Application application, File baseDir, boolean includeRoot, boolean includeDetailChildren,
             File... excludes) {
        DefaultSelectionProvider selectionProvider = new DefaultSelectionProvider("pages");
        if(includeRoot) {
            Page rootPage;
            try {
                rootPage = getPage(baseDir);
            } catch (Exception e) {
                throw new RuntimeException("Couldn't load root page", e);
            }
            selectionProvider.appendRow("/", rootPage.getTitle() + " (top level)", true);
        }
        appendChildrenToPagesSelectionProvider
                (application, baseDir, baseDir, null, selectionProvider, includeDetailChildren, excludes);
        return selectionProvider;
    }

    protected static void appendChildrenToPagesSelectionProvider
            (Application application, File baseDir, File parentDir, String breadcrumb,
             DefaultSelectionProvider selectionProvider, boolean includeDetailChildren, File... excludes) {
        FileFilter filter = new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        };
        for (File dir : parentDir.listFiles(filter)) {
            appendToPagesSelectionProvider
                    (application, baseDir, dir, breadcrumb, selectionProvider, includeDetailChildren, excludes);
        }
    }

    private static void appendToPagesSelectionProvider
            (Application application, File baseDir, File file, String breadcrumb,
             DefaultSelectionProvider selectionProvider, boolean includeDetailChildren, File... excludes) {
        if(ArrayUtils.contains(excludes, file)) {
            return;
        }
        if(PageInstance.DETAIL.equals(file.getName())) {
            if(includeDetailChildren) {
                breadcrumb += " (detail)"; //TODO I18n
                selectionProvider.appendRow
                    ("/" + ElementsFileUtils.getRelativePath(baseDir, file), breadcrumb, true);
                appendChildrenToPagesSelectionProvider
                        (application, baseDir, file, breadcrumb, selectionProvider, includeDetailChildren, excludes);
            }
        } else {
            Page page;
            try {
                page = getPage(file);
            } catch (Exception e) {
                throw new RuntimeException("Couldn't load page", e);
            }
            if (breadcrumb == null) {
                breadcrumb = page.getTitle();
            } else {
                breadcrumb = String.format("%s > %s", breadcrumb, page.getTitle());
            }
            selectionProvider.appendRow
                    ("/" + ElementsFileUtils.getRelativePath(baseDir, file), breadcrumb, true);
            appendChildrenToPagesSelectionProvider
                    (application, baseDir, file, breadcrumb, selectionProvider, includeDetailChildren, excludes);
        }
    }

    protected static final JAXBContext pagesJaxbContext;

    static {
        try {
            pagesJaxbContext = JAXBContext.newInstance(Page.class.getPackage().getName());
        } catch (JAXBException e) {
            throw new Error("Can't instantiate pages jaxb context", e);
        }
    }

    public static File savePage(PageInstance pageInstance) throws Exception {
        return savePage(pageInstance.getDirectory(), pageInstance.getPage());
    }

    public static File savePage(File directory, Page page) throws Exception {
        File pageFile = new File(directory, "page.xml");
        Marshaller marshaller = pagesJaxbContext.createMarshaller();
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(page, pageFile);
        return pageFile;
    }

    public static Page loadPage(File directory) throws Exception {
        File pageFile = new File(directory, "page.xml");
        FileInputStream fileInputStream = new FileInputStream(pageFile);
        try {
            return loadPage(fileInputStream);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }

    public static Page loadPage(InputStream inputStream) throws JAXBException {
        Unmarshaller unmarshaller = pagesJaxbContext.createUnmarshaller();
        return (Page) unmarshaller.unmarshal(inputStream);
    }

    public static Page getPage(File directory) throws Exception {
        Page page = loadPage(directory);
        page.init();
        return page;
    }

    public static File saveConfiguration(File directory, Object configuration) throws Exception {
        String configurationPackage = configuration.getClass().getPackage().getName();
        JAXBContext jaxbContext = JAXBContext.newInstance(configurationPackage);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        File configurationFile = new File(directory, "configuration.xml");
        marshaller.marshal(configuration, configurationFile);
        return configurationFile;
    }

    public static <T> T loadConfiguration(File directory, Class<? extends T> configurationClass) throws Exception {
        if(configurationClass == null) {
            return null;
        }
        File configurationFile = new File(directory, "configuration.xml");
        InputStream inputStream = new FileInputStream(configurationFile);
        try {
            return loadConfiguration(inputStream, configurationClass);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public static <T> T loadConfiguration
            (InputStream inputStream, Class<? extends T> configurationClass) throws Exception {
        if(configurationClass == null) {
            return null;
        }
        Object configuration;
        String configurationPackage = configurationClass.getPackage().getName();
        JAXBContext jaxbContext = JAXBContext.newInstance(configurationPackage);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        configuration = unmarshaller.unmarshal(inputStream);
        if(!configurationClass.isInstance(configuration)) {
            logger.error("Invalid configuration: expected " + configurationClass + ", got " + configuration);
            return null;
        }
        return (T) configuration;
    }

    public static Class<? extends PageAction> getActionClass(Application application, File directory) {
        File scriptFile = ScriptingUtil.getGroovyScriptFile(directory, "action");
        Class<? extends PageAction> actionClass = getCachedActionClass(scriptFile);
        if(actionClass != null) {
            return actionClass;
        } else {
            try {
                actionClass = (Class<? extends PageAction>) ScriptingUtil.getGroovyClass(scriptFile);
            } catch (Exception e) {
                logger.error("Couldn't load action class for " + directory + ", returning safe-mode action", e);
                return getFallbackActionClass(application);
            }
            if(isValidActionClass(actionClass)) {
                cacheActionClass(scriptFile, actionClass);
                return actionClass;
            } else {
                logger.error("Invalid action class for " + directory + ": " + actionClass);
                return getFallbackActionClass(application);
            }
        }
    }

    protected static Class<? extends PageAction> getFallbackActionClass(Application application) {
        Configuration configuration = application.getPortofinoProperties();
        String className = configuration.getString(PortofinoProperties.FALLBACK_ACTION_CLASS);
        try {
            Class<?> aClass = Class.forName(className);
            if(isValidActionClass(aClass)) {
                return (Class<? extends PageAction>) aClass;
            } else {
                throw new Error("Configuration error, invalid fallback action class: " + className);
            }
        } catch (Throwable e) {
            throw new Error("Configuration error, fallback action class not found: " + className, e);
        }
    }

    public static Class<? extends PageAction> setActionClass(File directory) throws IOException {
        File groovyScriptFile =
                ScriptingUtil.getGroovyScriptFile(directory, "action");
        Class<?> scriptClass = ScriptingUtil.getGroovyClass(groovyScriptFile);
        if(!isValidActionClass(scriptClass)) {
            removeCachedActionClass(groovyScriptFile);
            return null;
        } else {
            cacheActionClass(groovyScriptFile, (Class<? extends PageAction>) scriptClass);
            return (Class<? extends PageAction>) scriptClass;
        }
    }

    private static final ConcurrentMap<File, ActionClassInfo> actionClassCache =
            new ConcurrentHashMap<File, ActionClassInfo>();

    protected static class ActionClassInfo {
        public final Class<? extends PageAction> actionClass;
        public final long timestamp;
        public ActionClassInfo(Class<? extends PageAction> actionClass, long timestamp) {
            this.actionClass = actionClass;
            this.timestamp = timestamp;
        }
    }

    protected static Class<? extends PageAction> getCachedActionClass(File scriptFile) {
        ActionClassInfo info = actionClassCache.get(scriptFile);
        if(info != null) {
            logger.debug("Action found in cache: {}", scriptFile);
            if(info.timestamp < scriptFile.lastModified()) {
                logger.debug("Reloading action from {}", scriptFile);
                Class<? extends PageAction> actionClass;
                try {
                    actionClass = (Class<? extends PageAction>) ScriptingUtil.getGroovyClass(scriptFile);
                    cacheActionClass(scriptFile, actionClass);
                    return actionClass;
                } catch (Exception e) {
                    logger.error("Couldn't reload action class from " + scriptFile, e);
                    cacheActionClass(scriptFile, null);
                    return null;
                }
            } else {
                return info.actionClass;
            }
        } else {
            return null;
        }
    }

    protected static void cacheActionClass(File scriptFile, Class<? extends PageAction> scriptClass) {
        actionClassCache.put(scriptFile, new ActionClassInfo(scriptClass, scriptFile.lastModified()));
    }

    protected static void removeCachedActionClass(File scriptFile) {
        actionClassCache.remove(scriptFile);
    }

    public static boolean isValidActionClass(Class<?> actionClass) {
        if(actionClass == null) {
            return false;
        }
        if(!PageAction.class.isAssignableFrom(actionClass)) {
            logger.error("Action " + actionClass + " must implement " + PageAction.class);
            return false;
        }
        return true;
    }
}
